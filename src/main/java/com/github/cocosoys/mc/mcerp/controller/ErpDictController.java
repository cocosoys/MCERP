package com.github.cocosoys.mc.mcerp.controller;

import com.github.cocosoys.mc.mcerp.entity.ErpDictData;
import com.github.cocosoys.mc.mcerp.entity.ErpDictType;
import com.github.cocosoys.mc.mcerp.service.ErpDictService;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
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

/**
 * 字典管理（若依契约）：路由 /api/plugins/MCERP/system/dict/*。
 * 类型/数据 CRUD 全部委托 {@link ErpDictService}。
 */
@RequestMapping("/system/dict")
/**
 * 字典控制器：字典类型与字典数据的 CRUD。
 */
public class ErpDictController {

    private final ErpDictService dictService;

    public ErpDictController(ErpDictService dictService) {
        this.dictService = dictService;
    }

    // ===== 字典类型 =====

    @ApiName("字典类型列表")
    @ApiPermission("mcerp:system:dict:list")
    @GetMapping("/type/list")
    public TableDataInfo typeList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                  @RequestParam(name = "dictName", required = false) String dictName,
                                  @RequestParam(name = "dictType", required = false) String dictType) {
        return dictService.typeList(pageNum, pageSize, dictName, dictType);
    }

    @ApiName("字典类型详情")
    @ApiPermission("mcerp:system:dict:query")
    @GetMapping("/type/{dictId}")
    public AjaxResult typeDetail(@PathVariable(name = "dictId") String dictId) {
        return dictService.typeDetail(dictId);
    }

    @ApiName("新增字典类型")
    @ApiPermission("mcerp:system:dict:add")
    @PostMapping("/type")
    public AjaxResult typeAdd(@RequestBody ErpDictType body) {
        return dictService.typeAdd(body);
    }

    @ApiName("编辑字典类型")
    @ApiPermission("mcerp:system:dict:edit")
    @PutMapping("/type/{dictId}")
    public AjaxResult typeUpdate(@PathVariable(name = "dictId") String dictId, @RequestBody ErpDictType body) {
        return dictService.typeUpdate(dictId, body);
    }

    @ApiName("删除字典类型")
    @ApiPermission("mcerp:system:dict:remove")
    @DeleteMapping("/type/{dictIds}")
    public AjaxResult typeRemove(@PathVariable(name = "dictIds") String dictIds) {
        return dictService.typeRemove(dictIds);
    }

    @ApiName("字典选项")
    @ApiPermission("mcerp:system:dict:query")
    @GetMapping("/type/optionselect")
    public AjaxResult optionselect() {
        return dictService.optionselect();
    }

    @ApiName("刷新字典缓存")
    @ApiPermission("mcerp:system:dict:remove")
    @GetMapping("/type/refreshCache")
    public AjaxResult refreshCache() {
        return dictService.refreshCache();
    }

    // ===== 字典数据 =====

    @ApiName("字典数据列表")
    @ApiPermission("mcerp:system:dict:list")
    @GetMapping("/data/list")
    public TableDataInfo dataList(@RequestParam(name = "pageNum", required = false) Integer pageNum,
                                  @RequestParam(name = "pageSize", required = false) Integer pageSize,
                                  @RequestParam(name = "dictType", required = false) String dictType,
                                  @RequestParam(name = "dictLabel", required = false) String dictLabel) {
        return dictService.dataList(pageNum, pageSize, dictType, dictLabel);
    }

    @ApiName("按类型取字典数据")
    @ApiPermission("mcerp:system:dict:query")
    @GetMapping("/data/type/{dictType}")
    public AjaxResult dataByType(@PathVariable(name = "dictType") String dictType) {
        return dictService.dataByType(dictType);
    }

    @ApiName("字典数据详情")
    @ApiPermission("mcerp:system:dict:query")
    @GetMapping("/data/{dictCode}")
    public AjaxResult dataDetail(@PathVariable(name = "dictCode") String dictCode) {
        return dictService.dataDetail(dictCode);
    }

    @ApiName("新增字典数据")
    @ApiPermission("mcerp:system:dict:add")
    @PostMapping("/data")
    public AjaxResult dataAdd(@RequestBody ErpDictData body) {
        return dictService.dataAdd(body);
    }

    @ApiName("编辑字典数据")
    @ApiPermission("mcerp:system:dict:edit")
    @PutMapping("/data/{dictCode}")
    public AjaxResult dataUpdate(@PathVariable(name = "dictCode") String dictCode, @RequestBody ErpDictData body) {
        return dictService.dataUpdate(dictCode, body);
    }

    @ApiName("删除字典数据")
    @ApiPermission("mcerp:system:dict:remove")
    @DeleteMapping("/data/{dictCodes}")
    public AjaxResult dataRemove(@PathVariable(name = "dictCodes") String dictCodes) {
        return dictService.dataRemove(dictCodes);
    }
}
