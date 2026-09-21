package com.github.cocosoys.mc.mcerp.impl;

import static com.github.cocosoys.mc.mcerp.i18n.McerpI18n.t;

import com.github.cocosoys.mc.mcerp.entity.ErpDictData;
import com.github.cocosoys.mc.mcerp.entity.ErpDictType;
import com.github.cocosoys.mc.mcerp.entity.vo.DictOptionVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.ErpDictService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 字典管理实现（从 ErpDictController 迁入）：类型/数据 CRUD；
 * 删除类型时连带删除该类型下所有字典数据。
 */
public class ErpDictServiceImpl implements ErpDictService {

    private final OperLogService operLog;

    public ErpDictServiceImpl(OperLogService operLog) {
        this.operLog = operLog;
    }

    // ===== 字典类型 =====

    @Override
    public TableDataInfo typeList(Integer pageNum, Integer pageSize, String dictName, String dictType) {
        List<ErpDictType> all = DATA.select(ErpDictType.class);
        List<ErpDictType> filtered = new ArrayList<>();
        for (ErpDictType t : all) {
            if (dictName != null && !dictName.isEmpty() && !contains(t.getDictName(), dictName)) {
                continue;
            }
            if (dictType != null && !dictType.isEmpty() && !contains(t.getDictType(), dictType)) {
                continue;
            }
            filtered.add(t);
        }
        filtered.sort(Comparator.comparing(ErpDictType::getCreateTime, Comparator.nullsLast(Date::compareTo)).reversed());
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult typeDetail(String dictId) {
        ErpDictType dt = DATA.get(ErpDictType.class, dictId);
        if (dt == null) {
            return AjaxResult.error(t("mcerp.dict.type-not-found", "字典类型不存在"));
        }
        return AjaxResult.success(dt);
    }

    @Override
    public AjaxResult typeAdd(ErpDictType body) {
        String type = body.getDictType() == null ? "" : body.getDictType().trim();
        if (type.isEmpty()) {
            return AjaxResult.error(t("mcerp.dict.type-code-empty", "字典类型编码不能为空"));
        }
        for (ErpDictType t : DATA.select(ErpDictType.class)) {
            if (type.equals(t.getDictType())) {
                return AjaxResult.error(t("mcerp.dict.type-exists", "字典类型已存在"));
            }
        }
        ErpDictType dt = new ErpDictType();
        dt.setDictId(UUID.randomUUID().toString());
        dt.setDictName(body.getDictName() == null || body.getDictName().isEmpty() ? type : body.getDictName());
        dt.setDictType(type);
        dt.setStatus(body.getStatus() == null || body.getStatus().isEmpty() ? "0" : body.getStatus());
        dt.setRemark(body.getRemark() == null ? "" : body.getRemark());
        dt.setCreateTime(AuthService.now());
        DATA.insert(dt);
        operLog.record(t("mcerp.operlog.module.dict", "字典管理"), t("mcerp.operlog.action.add-dict-type", "新增字典类型"), type, t("mcerp.common.add-success", "新增成功"));
        return AjaxResult.success(t("mcerp.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult typeUpdate(String dictId, ErpDictType body) {
        ErpDictType dt = dictId == null || dictId.isEmpty() ? null : DATA.get(ErpDictType.class, dictId);
        if (dt == null) {
            return AjaxResult.error(t("mcerp.dict.type-not-found", "字典类型不存在"));
        }
        if (body.getDictName() != null) {
            dt.setDictName(body.getDictName());
        }
        if (body.getDictType() != null) {
            dt.setDictType(body.getDictType());
        }
        if (body.getStatus() != null) {
            dt.setStatus(body.getStatus());
        }
        if (body.getRemark() != null) {
            dt.setRemark(body.getRemark());
        }
        DATA.updateById(dt);
        operLog.record(t("mcerp.operlog.module.dict", "字典管理"), t("mcerp.operlog.action.edit-dict-type", "修改字典类型"), dt.getDictType(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult typeRemove(String dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return AjaxResult.error(t("mcerp.dict.missing-id", "缺少 dictId"));
        }
        for (String id : dictIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            ErpDictType dt = DATA.get(ErpDictType.class, id.trim());
            DATA.deleteById(ErpDictType.class, id.trim());
            if (dt != null) {
                // 连带删除该类型下的字典数据
                for (ErpDictData d : DATA.select(ErpDictData.class)) {
                    if (dt.getDictType().equals(d.getDictType())) {
                        DATA.deleteById(ErpDictData.class, d.getDictCode());
                    }
                }
                operLog.record(t("mcerp.operlog.module.dict", "字典管理"), t("mcerp.operlog.action.delete-dict-type", "删除字典类型"), dt.getDictType(), t("mcerp.common.delete-success", "删除成功"));
            }
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    @Override
    public AjaxResult optionselect() {
        return AjaxResult.success(DATA.select(ErpDictType.class));
    }

    @Override
    public AjaxResult refreshCache() {
        return AjaxResult.success(t("mcerp.common.refresh-success", "刷新成功"));
    }

    // ===== 字典数据 =====

    @Override
    public TableDataInfo dataList(Integer pageNum, Integer pageSize, String dictType, String dictLabel) {
        List<ErpDictData> all = DATA.select(ErpDictData.class);
        List<ErpDictData> filtered = new ArrayList<>();
        for (ErpDictData d : all) {
            if (dictType != null && !dictType.isEmpty() && !dictType.equals(d.getDictType())) {
                continue;
            }
            if (dictLabel != null && !dictLabel.isEmpty() && !contains(d.getDictLabel(), dictLabel)) {
                continue;
            }
            filtered.add(d);
        }
        filtered.sort(Comparator.comparingInt(ErpDictData::getDictSort));
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult dataByType(String dictType) {
        List<DictOptionVO> out = new ArrayList<>();
        for (ErpDictData d : DATA.select(ErpDictData.class)) {
            if (dictType != null && dictType.equals(d.getDictType())) {
                DictOptionVO vo = new DictOptionVO();
                vo.setDictLabel(d.getDictLabel());
                vo.setDictValue(d.getDictValue());
                vo.setDictType(d.getDictType());
                out.add(vo);
            }
        }
        return AjaxResult.success(out);
    }

    @Override
    public AjaxResult dataDetail(String dictCode) {
        ErpDictData d = DATA.get(ErpDictData.class, dictCode);
        if (d == null) {
            return AjaxResult.error(t("mcerp.dict.data-not-found", "字典数据不存在"));
        }
        return AjaxResult.success(d);
    }

    @Override
    public AjaxResult dataAdd(ErpDictData body) {
        ErpDictData d = new ErpDictData();
        d.setDictCode(UUID.randomUUID().toString());
        d.setDictSort(body.getDictSort());
        d.setDictLabel(body.getDictLabel() == null ? "" : body.getDictLabel());
        d.setDictValue(body.getDictValue() == null ? "" : body.getDictValue());
        d.setDictType(body.getDictType() == null ? "" : body.getDictType());
        d.setStatus(body.getStatus() == null || body.getStatus().isEmpty() ? "0" : body.getStatus());
        d.setRemark(body.getRemark() == null ? "" : body.getRemark());
        if (d.getDictType().isEmpty()) {
            return AjaxResult.error(t("mcerp.dict.type-empty", "字典类型不能为空"));
        }
        DATA.insert(d);
        operLog.record(t("mcerp.operlog.module.dict", "字典管理"), t("mcerp.operlog.action.add-dict-data", "新增字典数据"), d.getDictLabel(), t("mcerp.common.add-success", "新增成功"));
        return AjaxResult.success(t("mcerp.common.add-success", "新增成功"));
    }

    @Override
    public AjaxResult dataUpdate(String dictCode, ErpDictData body) {
        ErpDictData d = dictCode == null || dictCode.isEmpty() ? null : DATA.get(ErpDictData.class, dictCode);
        if (d == null) {
            return AjaxResult.error(t("mcerp.dict.data-not-found", "字典数据不存在"));
        }
        d.setDictSort(body.getDictSort());
        if (body.getDictLabel() != null) {
            d.setDictLabel(body.getDictLabel());
        }
        if (body.getDictValue() != null) {
            d.setDictValue(body.getDictValue());
        }
        if (body.getDictType() != null) {
            d.setDictType(body.getDictType());
        }
        if (body.getStatus() != null) {
            d.setStatus(body.getStatus());
        }
        if (body.getRemark() != null) {
            d.setRemark(body.getRemark());
        }
        DATA.updateById(d);
        operLog.record(t("mcerp.operlog.module.dict", "字典管理"), t("mcerp.operlog.action.edit-dict-data", "修改字典数据"), d.getDictLabel(), t("mcerp.common.edit-success", "修改成功"));
        return AjaxResult.success(t("mcerp.common.edit-success", "修改成功"));
    }

    @Override
    public AjaxResult dataRemove(String dictCodes) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return AjaxResult.error(t("mcerp.dict.missing-code", "缺少 dictCode"));
        }
        for (String id : dictCodes.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            ErpDictData d = DATA.get(ErpDictData.class, id.trim());
            DATA.deleteById(ErpDictData.class, id.trim());
            if (d != null) {
                operLog.record(t("mcerp.operlog.module.dict", "字典管理"), t("mcerp.operlog.action.delete-dict-data", "删除字典数据"), d.getDictLabel(), t("mcerp.common.delete-success", "删除成功"));
            }
        }
        return AjaxResult.success(t("mcerp.common.delete-success", "删除成功"));
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
