package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.entity.vo.ErpMenuVO;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPublic;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.Hidden;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller 自动按钮扫描器：把 controller 类的端点方法投影为 {@code F}（按钮）菜单节点，
 * 免去在 {@link ErpMenus} 中逐个手写 {@code perm(...)}。
 *
 * <p>规则（与主插件 {@code ApiRegistry} 的端点解析语义保持一致）：</p>
 * <ul>
 *   <li><b>端点方法</b>：方法上存在 {@link GetMapping}/{@link PostMapping}/{@link PutMapping}/{@link DeleteMapping}
 *       之一才算端点，其余方法忽略；</li>
 *   <li><b>名称</b>：按钮名 = 方法级 {@link ApiName} → 回退类级 {@link ApiName} → 回退 Java 方法名；</li>
 *   <li><b>权限</b>：按钮 perms = 方法级 {@link ApiPermission} → 回退类级 {@link ApiPermission}；
 *       {@link ApiPublic}（方法级或类级）时按钮<b>不设权限</b>（perms 置空，登录即可）；</li>
 *   <li><b>默认拒绝端点跳过</b>：既无 {@link ApiPermission} 又无 {@link ApiPublic} 的端点
 *       （网关默认拒绝 403）不生成按钮——无效按钮不展示；</li>
 *   <li><b>{@link Hidden} 跳过</b>：方法级或类级 {@link Hidden} 的端点不生成按钮（隐藏端点不展示）；</li>
 *   <li><b>menuId</b>：有权限用权限串（唯一、语义清晰），无权限（@{@link ApiPublic}）用 Java 方法名；</li>
 *   <li><b>排序</b>：按方法声明顺序递增（从 startOrder 起）。</li>
 * </ul>
 *
 * <p>通常不直接调用：经 {@link ErpMenus#menu(String, String, String, Class[]...)} 重载或
 * {@link ErpMenus#permsFrom(Class[]...)} 链式接入。</p>
 */
public final class ControllerPermScanner {

    /** 工具类：禁止实例化。 */
    private ControllerPermScanner() {
    }

    /**
     * 扫描 controller 类的全部端点方法，生成 F 按钮列表（排序从 0 起）。
     *
     * @param controller controller 类（可空：返回空列表）
     * @return F 按钮节点列表
     */
    public static List<ErpMenuVO> scan(Class<?> controller) {
        return scan(controller, 0);
    }

    /**
     * 扫描 controller 类的全部端点方法，生成 F 按钮列表。
     *
     * @param controller controller 类（可空：返回空列表）
     * @param startOrder 起始排序号（多 controller 顺序拼接时传上一批的结束值）
     * @return F 按钮节点列表
     */
    public static List<ErpMenuVO> scan(Class<?> controller, int startOrder) {
        List<ErpMenuVO> out = new ArrayList<>();
        if (controller == null) {
            return out;
        }
        if (controller.isAnnotationPresent(Hidden.class)) {
            return out; // 类级 @Hidden：整类端点隐藏，不生成按钮
        }
        String clsName = classApiName(controller);
        String clsPermission = classApiPermission(controller);
        boolean clsPublic = controller.isAnnotationPresent(ApiPublic.class);
        int order = startOrder;
        for (Method m : controller.getDeclaredMethods()) {
            if (!isEndpoint(m)) {
                continue;
            }
            if (m.isAnnotationPresent(Hidden.class)) {
                continue; // 方法级 @Hidden：隐藏端点不生成按钮
            }
            ApiPermission ap = m.getAnnotation(ApiPermission.class);
            String perms = firstNonEmpty(ap == null ? null : ap.value(), clsPermission);
            boolean isPublic = m.isAnnotationPresent(ApiPublic.class) || clsPublic;
            if (!isPublic && (perms == null || perms.isEmpty())) {
                continue; // 默认拒绝端点（网关 403），不生成无效按钮
            }
            ErpMenuVO btn = new ErpMenuVO();
            btn.setMenuId(perms != null && !perms.isEmpty() ? perms : m.getName());
            ApiName an = m.getAnnotation(ApiName.class);
            btn.setMenuName(firstNonEmpty(an == null ? null : an.value(), clsName, m.getName()));
            btn.setMenuType("F");
            btn.setPerms(isPublic ? null : perms); // @ApiPublic → 不设权限（登录即可）
            btn.setOrderNum(order++);
            out.add(btn);
        }
        return out;
    }

    // ===== 内部 =====

    /** 端点判定：方法上存在 4 个 HTTP 映射注解之一。 */
    private static boolean isEndpoint(Method m) {
        return m.isAnnotationPresent(GetMapping.class)
                || m.isAnnotationPresent(PostMapping.class)
                || m.isAnnotationPresent(PutMapping.class)
                || m.isAnnotationPresent(DeleteMapping.class);
    }

    /** 类级 @ApiName（可空）。 */
    private static String classApiName(Class<?> cls) {
        ApiName an = cls.getAnnotation(ApiName.class);
        return an == null ? null : an.value();
    }

    /** 类级 @ApiPermission（可空）。 */
    private static String classApiPermission(Class<?> cls) {
        ApiPermission ap = cls.getAnnotation(ApiPermission.class);
        return ap == null ? null : ap.value();
    }

    /** 取第一个非空值（方法级 → 类级 → 兜底）。 */
    private static String firstNonEmpty(String... values) {
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                return v;
            }
        }
        return null;
    }
}
