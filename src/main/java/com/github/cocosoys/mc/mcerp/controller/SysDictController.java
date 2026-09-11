package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.SysDictData;
import com.github.cocosoys.mc.mcerp.entity.SysDictType;
import com.github.cocosoys.mc.mcerp.service.Store;
import com.github.cocosoys.mc.mcerp.util.Json;
import com.github.cocosoys.mc.mcerp.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiName;
import com.github.cocosoys.mc.soyshttpovermc.annotations.ApiPermission;
import com.github.cocosoys.mc.soyshttpovermc.annotations.DeleteMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.GetMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PathVariable;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PostMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.PutMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestBody;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestMapping;
import com.github.cocosoys.mc.soyshttpovermc.annotations.RequestParam;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 字典管理（若依契约：经 registerProxyController 代理注册，SOYS 自动补 /api 全局前缀
 * → 实际路由 /api/prod-api/system/dict/*）：字典类型 + 字典数据。
 */
@RequestMapping("/prod-api/system/dict")
public class SysDictController {

    public SysDictController() {
    }

    /** 内置常用字典（首次启动写入） */
    public void ensureBuiltinDicts() {
        ensureType("system_normal_disable", "系统开关", new String[][]{{"0", "正常"}, {"1", "停用"}});
        ensureType("sys_yes_no", "系统是否", new String[][]{{"Y", "是"}, {"N", "否"}});
        ensureType("sys_user_sex", "用户性别", new String[][]{{"0", "男"}, {"1", "女"}, {"2", "未知"}});
    }

    private void ensureType(String type, String name, String[][] data) {
        for (SysDictType t : Store.select(SysDictType.class)) {
            if (type.equals(t.getDictType())) {
                return;
            }
        }
        SysDictType dt = new SysDictType();
        dt.setDictId(UUID.randomUUID().toString());
        dt.setDictName(name);
        dt.setDictType(type);
        dt.setStatus("0");
        dt.setRemark("系统内置字典");
        dt.setCreateTime(com.github.cocosoys.mc.mcerp.service.AuthService.now());
        Store.insert(dt);
        for (String[] pair : data) {
            SysDictData d = new SysDictData();
            d.setDictCode(UUID.randomUUID().toString());
            d.setDictSort(0);
            d.setDictLabel(pair[1]);
            d.setDictValue(pair[0]);
            d.setDictType(type);
            d.setStatus("0");
            Store.insert(d);
        }
    }

    // ===== 字典类型 =====

    @ApiName("字典类型列表")
    @ApiPermission("system:dict:list")
    @GetMapping("/type/list")
    public TableDataInfo typeList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                  @RequestParam(name = "dictName", required = false) String dictName,
                                  @RequestParam(name = "dictType", required = false) String dictType) {
        List<SysDictType> all = Store.select(SysDictType.class);
        List<SysDictType> filtered = new ArrayList<>();
        for (SysDictType t : all) {
            if (dictName != null && !dictName.isEmpty() && !contains(t.getDictName(), dictName)) {
                continue;
            }
            if (dictType != null && !dictType.isEmpty() && !contains(t.getDictType(), dictType)) {
                continue;
            }
            filtered.add(t);
        }
        filtered.sort(Comparator.comparing(SysDictType::getCreateTime, Comparator.nullsLast(String::compareTo)).reversed());
        return SysUserController.page(filtered, pageNum, pageSize);
    }

    @ApiName("字典类型详情")
    @ApiPermission("system:dict:query")
    @GetMapping("/type/{dictId}")
    public AjaxResult typeDetail(@PathVariable(name = "dictId") String dictId) {
        SysDictType dt = Store.get(SysDictType.class, dictId);
        if (dt == null) {
            return AjaxResult.error("字典类型不存在");
        }
        return AjaxResult.success(dt);
    }

    @ApiName("新增字典类型")
    @ApiPermission("system:dict:add")
    @PostMapping("/type")
    public AjaxResult typeAdd(@RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        String type = Json.getString(map, "dictType", "");
        if (type.isEmpty()) {
            return AjaxResult.error("字典类型编码不能为空");
        }
        for (SysDictType t : Store.select(SysDictType.class)) {
            if (type.equals(t.getDictType())) {
                return AjaxResult.error("字典类型已存在");
            }
        }
        SysDictType dt = new SysDictType();
        dt.setDictId(UUID.randomUUID().toString());
        dt.setDictName(Json.getString(map, "dictName", type));
        dt.setDictType(type);
        dt.setStatus(Json.getString(map, "status", "0"));
        dt.setRemark(Json.getString(map, "remark", ""));
        dt.setCreateTime(com.github.cocosoys.mc.mcerp.service.AuthService.now());
        Store.insert(dt);
        SysUserController.recordOper("字典管理", "新增字典类型", type, "新增成功");
        return AjaxResult.success("新增成功");
    }

    @ApiName("编辑字典类型")
    @ApiPermission("system:dict:edit")
    @PutMapping("/type/{dictId}")
    public AjaxResult typeUpdate(@PathVariable(name = "dictId") String dictId, @RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        SysDictType dt = dictId == null || dictId.isEmpty() ? null : Store.get(SysDictType.class, dictId);
        if (dt == null) {
            return AjaxResult.error("字典类型不存在");
        }
        dt.setDictName(Json.getString(map, "dictName", dt.getDictName()));
        dt.setDictType(Json.getString(map, "dictType", dt.getDictType()));
        dt.setStatus(Json.getString(map, "status", dt.getStatus()));
        dt.setRemark(Json.getString(map, "remark", dt.getRemark()));
        Store.updateById(dt);
        SysUserController.recordOper("字典管理", "修改字典类型", dt.getDictType(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("删除字典类型")
    @ApiPermission("system:dict:remove")
    @DeleteMapping("/type/{dictIds}")
    public AjaxResult typeRemove(@PathVariable(name = "dictIds") String dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return AjaxResult.error("缺少 dictId");
        }
        for (String id : dictIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysDictType dt = Store.get(SysDictType.class, id.trim());
            Store.deleteById(SysDictType.class, id.trim());
            if (dt != null) {
                // 连带删除该类型下的字典数据
                for (SysDictData d : Store.select(SysDictData.class)) {
                    if (dt.getDictType().equals(d.getDictType())) {
                        Store.deleteById(SysDictData.class, d.getDictCode());
                    }
                }
                SysUserController.recordOper("字典管理", "删除字典类型", dt.getDictType(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    @ApiName("字典选项")
    @ApiPermission("system:dict:query")
    @GetMapping("/type/optionselect")
    public AjaxResult optionselect() {
        return AjaxResult.success(Store.select(SysDictType.class));
    }

    @ApiName("刷新字典缓存")
    @ApiPermission("system:dict:remove")
    @GetMapping("/type/refreshCache")
    public AjaxResult refreshCache() {
        return AjaxResult.success("刷新成功");
    }

    // ===== 字典数据 =====

    @ApiName("字典数据列表")
    @ApiPermission("system:dict:list")
    @GetMapping("/data/list")
    public TableDataInfo dataList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                  @RequestParam(name = "dictType", required = false) String dictType,
                                  @RequestParam(name = "dictLabel", required = false) String dictLabel) {
        List<SysDictData> all = Store.select(SysDictData.class);
        List<SysDictData> filtered = new ArrayList<>();
        for (SysDictData d : all) {
            if (dictType != null && !dictType.isEmpty() && !dictType.equals(d.getDictType())) {
                continue;
            }
            if (dictLabel != null && !dictLabel.isEmpty() && !contains(d.getDictLabel(), dictLabel)) {
                continue;
            }
            filtered.add(d);
        }
        filtered.sort(Comparator.comparingInt(SysDictData::getDictSort));
        return SysUserController.page(filtered, pageNum, pageSize);
    }

    @ApiName("按类型取字典数据")
    @ApiPermission("system:dict:query")
    @GetMapping("/data/type/{dictType}")
    public AjaxResult dataByType(@PathVariable(name = "dictType") String dictType) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (SysDictData d : Store.select(SysDictData.class)) {
            if (dictType != null && dictType.equals(d.getDictType())) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("dictLabel", d.getDictLabel());
                m.put("dictValue", d.getDictValue());
                m.put("dictType", d.getDictType());
                out.add(m);
            }
        }
        return AjaxResult.success(out);
    }

    @ApiName("字典数据详情")
    @ApiPermission("system:dict:query")
    @GetMapping("/data/{dictCode}")
    public AjaxResult dataDetail(@PathVariable(name = "dictCode") String dictCode) {
        SysDictData d = Store.get(SysDictData.class, dictCode);
        if (d == null) {
            return AjaxResult.error("字典数据不存在");
        }
        return AjaxResult.success(d);
    }

    @ApiName("新增字典数据")
    @ApiPermission("system:dict:add")
    @PostMapping("/data")
    public AjaxResult dataAdd(@RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        SysDictData d = new SysDictData();
        d.setDictCode(UUID.randomUUID().toString());
        d.setDictSort(Json.getInt(map, "dictSort", 0));
        d.setDictLabel(Json.getString(map, "dictLabel", ""));
        d.setDictValue(Json.getString(map, "dictValue", ""));
        d.setDictType(Json.getString(map, "dictType", ""));
        d.setStatus(Json.getString(map, "status", "0"));
        d.setRemark(Json.getString(map, "remark", ""));
        if (d.getDictType().isEmpty()) {
            return AjaxResult.error("字典类型不能为空");
        }
        Store.insert(d);
        SysUserController.recordOper("字典管理", "新增字典数据", d.getDictLabel(), "新增成功");
        return AjaxResult.success("新增成功");
    }

    @ApiName("编辑字典数据")
    @ApiPermission("system:dict:edit")
    @PutMapping("/data/{dictCode}")
    public AjaxResult dataUpdate(@PathVariable(name = "dictCode") String dictCode, @RequestBody String body) {
        Map<String, Object> map = Json.parseObject(body);
        SysDictData d = dictCode == null || dictCode.isEmpty() ? null : Store.get(SysDictData.class, dictCode);
        if (d == null) {
            return AjaxResult.error("字典数据不存在");
        }
        d.setDictSort(Json.getInt(map, "dictSort", d.getDictSort()));
        d.setDictLabel(Json.getString(map, "dictLabel", d.getDictLabel()));
        d.setDictValue(Json.getString(map, "dictValue", d.getDictValue()));
        d.setDictType(Json.getString(map, "dictType", d.getDictType()));
        d.setStatus(Json.getString(map, "status", d.getStatus()));
        d.setRemark(Json.getString(map, "remark", d.getRemark()));
        Store.updateById(d);
        SysUserController.recordOper("字典管理", "修改字典数据", d.getDictLabel(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @ApiName("删除字典数据")
    @ApiPermission("system:dict:remove")
    @DeleteMapping("/data/{dictCodes}")
    public AjaxResult dataRemove(@PathVariable(name = "dictCodes") String dictCodes) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return AjaxResult.error("缺少 dictCode");
        }
        for (String id : dictCodes.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysDictData d = Store.get(SysDictData.class, id.trim());
            Store.deleteById(SysDictData.class, id.trim());
            if (d != null) {
                SysUserController.recordOper("字典管理", "删除字典数据", d.getDictLabel(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
