package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;
import lombok.CustomLog;
import lombok.Getter;
import lombok.Setter;
import org.bukkit.plugin.Plugin;

import java.util.List;

/**
 * ERP 模块扩展基类（继承 SoysExpansion 泛化）。
 *
 * <p>附属 ERP 插件只需<b>继承本类 + 覆写 ERP 声明钩子 + 一行 {@code register()}</b>，
 * 即自动完成全部登记：
 * <ul>
 *   <li>SoysExpansion 骨架自动执行：端点批量登记（/api/plugins/&lt;id&gt;/*）、页面托管
 *       （/web/plugins/&lt;id&gt;/*）、数据层初始化（{@code dataRoots()/sqlRoots()/seedData()/
 *       schemaVersion()}，主插件内置事务 + meta 幂等）、CORS、冲突检测与失败回滚；</li>
 *   <li>本类 {@link #onRegister()} 默认实现：把模块（identifier/displayName/menus()/menusTable()…）
 *       登记到 MCERP {@link ErpRegistry} → 菜单/路由实时合成（即安即生效）；</li>
 *   <li>{@link #onUnregister()} 默认实现：摘除 ERP 登记（卸载同步清理）。</li>
 * </ul>
 *
 * <pre>
 * public class StockExpansion extends McerpExpansion {
 *     &#64;Override public String getIdentifier() { return "stock"; }
 *     &#64;Override protected String displayName() { return "库存管理"; }
 *     &#64;Override protected ErpMenus menus() {
 *         return ErpMenus.create()
 *             .dir("system", "系统管理", "system", d -> d
 *                 .menu("user", "用户列表", "user").perm("soys.erp.user.list")
 *                 .menu("group", "权限组列表", "lock"))
 *             .menu("home", "首页", "home").component("erp/home");
 *     }                                                        // 或 menusTable()
 *     &#64;Override protected String[] dataRoots() { return new String[]{"data"}; }  // 继承自 SoysExpansion
 * }
 * // onEnable:
 * if (!new StockExpansion().register()) {
 *     getLogger().warning("stock 注册失败（identifier 冲突 / bootstrap 未就绪）");
 * }
 * </pre>
 *
 * <p><b>MCERP 判定"是否 ERP 模块"</b> = {@code obj instanceof McerpExpansion}；
 * 宿主 {@link McErpHostExpansion} 是 SoysExpansion 直系子类，天然排除。
 * 未绑定 MCERP（未安装/未就绪）时 onRegister 静默跳过，由 /mcerp reload 或
 * /soyshttp reload 兜底补登记。</p>
 */
@CustomLog
public abstract class McerpExpansion extends SoysExpansion {

    /** MCERP 宿主引用（MCERP onEnable 时经 {@link #setMcerp(MCERP)} 注入；未绑定 = MCERP 未装/未就绪）。 */
    protected @Getter @Setter static volatile MCERP mcerp;

    // ===== ERP 声明钩子（全默认，覆写需要者）=====

    /** 菜单标题（默认 = identifier）。 */
    protected String displayName() {
        return getIdentifier();
    }

    /** 模块图标。 */
    protected String icon() {
        return null;
    }

    /** 排序号。 */
    protected int sortOrder() {
        return 0;
    }

    /** 主菜单访问权限标识。 */
    protected String permission() {
        return null;
    }

    /** 无子菜单时的默认页地址。 */
    protected String homeUrl() {
        return null;
    }

    /**
     * 菜单树（自带 component/perms/children），声明式构建器写法见 {@link ErpMenus}。
     * 菜单下的按钮可免手写：menu 声明带 controller 类或链式 {@code permsFrom(Class...)}，
     * 自动按 {@link ControllerPermScanner} 规则把端点方法投影为 F 按钮。
     * 选填项，若你希望通过数据库新增的方式，请向用户提供数据库代码(yml/sql)
     */
    protected ErpMenus menus() {
        return null;
    }

    /**
     * 菜单对照表（直接返回 {@link List}&lt;{@link ErpMenuVO}&gt; 树状结构；与 {@link #menus()} 二选一，{@link #menus()} 优先）。
     *
     * <p>与 DSL 构建器不同，本钩子让用户自行通过 {@code new ErpMenuVO()} 拼接树：
     * 逐节点设置表字段（menuId/menuName/menuType/parentId/orderNum/path/component/perms/icon/visible…），
     * 子节点放入父节点 {@code children} 列表即形成层级。
     * 选填项，若你希望通过数据库新增的方式，请向用户提供数据库代码(yml/sql)
     */
    protected List<ErpMenuVO> menusTable() {
        return null;
    }

    // ===== 生命周期：自动登记/摘除 MCERP =====

    @Override
    public boolean onRegister() {
        MCERP m = mcerp;
        if (m == null) {
            log.infoT("mcerp.log.expansion-skip", "MCERP 未就绪，跳过 ERP 登记（reload 时兜底补登记）: {0}", getIdentifier());
            return true; // 静默，不阻断 SoysExpansion 正常注册
        }
        ErpModuleVO vo = toModuleVO();
        if (vo == null || vo.getDisplayName() == null || vo.getDisplayName().trim().isEmpty()) {
            log.warnT("mcerp.log.expansion-no-displayname", "ERP 模块缺少 displayName，拒绝登记: {0}", getIdentifier());
            return false; // displayName 必填 → 整体回滚
        }
        Plugin owner = getOwner();
        if (owner == null) {
            log.warnT("mcerp.log.expansion-no-owner", "ERP 模块无法定位 owner 插件，拒绝登记: {0}", getIdentifier());
            return false;
        }
        // 写入索引（仅 identifier）：与 /mcerp reload 兜底同通道，幂等。
        // displayName / owner 校验仅用于决定是否阻断 SoysExpansion 整体注册。
        m.getRegistry().registerModule(this);
        return true;
    }

    @Override
    public void onUnregister() {
        MCERP m = mcerp;
        if (m == null) {
            return;
        }
        Plugin owner = getOwner();
        if (owner != null) {
            m.getRegistry().unregisterModule(owner.getName());
        }
    }

    // ===== 登记数据组装（onRegister 与 reload 兜底补登记共用）=====

    /**
     * 组装 ErpModuleVO（id = getIdentifier()，菜单 = menus() 或 menusTable()）。
     */
    public ErpModuleVO toModuleVO() {
        ErpModuleVO vo = new ErpModuleVO();
        vo.setId(getIdentifier());
        vo.setDisplayName(displayName());
        vo.setIcon(icon());
        vo.setHomeUrl(homeUrl());
        vo.setPermission(permission());
        vo.setSortOrder(sortOrder());
        ErpMenus ms = menus();
        List<ErpMenuVO> children = ms == null ? null : ms.build();
        if (children == null || children.isEmpty()) {
            List<ErpMenuVO> direct = menusTable();
            if (direct != null && !direct.isEmpty()) {
                children = direct;
            }
        }
        if (children != null) {
            vo.getChildren().addAll(children);
        }
        return vo;
    }
}
