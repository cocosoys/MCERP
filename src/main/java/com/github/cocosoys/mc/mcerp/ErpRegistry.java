package com.github.cocosoys.mc.mcerp;

import com.github.cocosoys.mc.mcerp.entity.vo.ErpModuleVO;
import com.github.cocosoys.mc.soyshttpovermc.api.SoysExpansion;
import lombok.CustomLog;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * ERP 模块索引（投影缓存，仅登记 identifier）。
 *
 * <p>设计理念：MCERP 是主插件 {@code SoysExpansion.REGISTERED} 的投影者——
 * 模块注册生命周期完全由主插件骨架管理（register/unregister/owner），本类只维护
 * "当前有哪些 ERP 模块"的轻量索引（identifier → owner 插件名）。模块内容
 * （menus()/routeTable()）在菜单/路由合成时<b>实时</b>从 REGISTERED 反查实例组装，
 * 不做任何内容缓存——无陈旧、无残留、单一事实源。</p>
 *
 * <ul>
 *   <li>onRegister → {@link #registerModule(McerpExpansion)} 写入索引</li>
 *   <li>onUnregister / 插件禁用 → {@link #unregisterModule(String)} 摘除索引</li>
 *   <li>/mcerp reload 与 /soyshttp reload → {@link #reloadExpansions(Set)} 清理残留 +
 *       全量重新填充索引</li>
 * </ul>
 */
@CustomLog
public class EripRegistry {

    /** 在线 ERP 模块索引：identifier → owner 插件名 */
    private final Map<String, String> dynamic = new LinkedHashMap<>();

    /**
     * 登记 ERP 模块（写入索引，仅 identifier → owner 插件名）。
     *
     * <p>基于当前投影索引架构：模块注册生命周期由主插件 {@code SoysExpansion.REGISTERED}
     * 管理（register/unregister/owner），本方法只记录"该模块属于 ERP 索引"的轻量标记
     * （identifier）；模块内容（menus()/routeTable()）在菜单/路由合成时实时从 REGISTERED
     * 反查实例组装，不做任何内容缓存。</p>
     *
     * <p>由 {@link McerpExpansion#onRegister()}（主通道）与 /mcerp reload、/soyshttp reload
     * （兜底）共用同一入口，幂等——重复登记仅在线确认。</p>
     *
     * @param exp ERP 模块扩展（McerpExpansion 子类）
     * @return 本次新增登记数（已登记返回 0）
     */
    public synchronized int registerModule(McerpExpansion exp) {
        if (exp == null) {
            return 0;
        }
        String id = exp.getIdentifier();
        if (id == null || id.trim().isEmpty()) {
            return 0;
        }
        id = id.trim();
        if (dynamic.containsKey(id)) {
            return 0; // 已登记 → 仅在线确认
        }
        Plugin owner = exp.getOwner();
        dynamic.put(id, owner == null ? "?" : owner.getName());
        log.info("ERP 模块已登记: " + id);
        return 1;
    }

    /**
     * 按插件名摘除其全部索引（插件禁用时调用）。
     */
    public synchronized void unregisterModule(String ownerPluginName) {
        boolean changed = false;
        Iterator<Map.Entry<String, String>> it = dynamic.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (e.getValue().equalsIgnoreCase(ownerPluginName)) {
                it.remove();
                changed = true;
            }
        }
        if (changed) {
            log.info("插件禁用，摘除其 ERP 登记: " + ownerPluginName);
        }
    }

    /**
     * 清理残留索引：移除已不在 SoysExpansion.REGISTERED 的条目。
     * 手动录入/时序残留由本次 reload 统一兜底清除。
     *
     * @param registeredIds 当前 SoysExpansion.REGISTERED 中全部 McerpExpansion 的 identifier
     * @return 移除的残留数
     */
    public synchronized int reloadExpansions(Set<String> registeredIds) {
        int removed = 0;
        Iterator<Map.Entry<String, String>> it = dynamic.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> e = it.next();
            if (!registeredIds.contains(e.getKey())) {
                it.remove();
                removed++;
            }
        }
        if (removed > 0) {
            log.info("ERP 重建：移除已不在线的模块索引 " + removed);
        }
        return removed;
    }

    /**
     * 已登记模块列表（按 sortOrder 升序）。
     *
     * <p>合成时按 id 实时从 {@code SoysExpansion.REGISTERED} 反查实例并组装 VO
     * （menus()/routeTable() 即时生效）；索引有但实例已不在（极端时序残留）→
     * 跳过该条，下次 reload 自动清。</p>
     */
    public synchronized List<ErpModuleVO> getModules() {
        List<ErpModuleVO> list = new ArrayList<>();
        for (String id : dynamic.keySet()) {
            SoysExpansion exp = SoysExpansion.registered().get(id);
            if (exp instanceof McerpExpansion) {
                ErpModuleVO vo = ((McerpExpansion) exp).toModuleVO();
                if (vo != null && vo.getId() != null) {
                    list.add(vo);
                }
            }
        }
        list.sort(Comparator.comparingInt(ErpModuleVO::getSortOrder));
        return list;
    }

    public synchronized ErpModuleVO getModule(String id) {
        for (ErpModuleVO m : getModules()) {
            if (m.getId().equalsIgnoreCase(id)) {
                return m;
            }
        }
        return null;
    }

    public synchronized boolean isRegistered(String id) {
        return dynamic.containsKey(id);
    }

    public synchronized List<String> getModuleIds() {
        return new ArrayList<>(dynamic.keySet());
    }
}
