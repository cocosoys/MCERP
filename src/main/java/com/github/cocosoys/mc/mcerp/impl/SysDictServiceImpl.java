package com.github.cocosoys.mc.mcerp.impl;

import com.github.cocosoys.mc.mcerp.entity.SysDictData;
import com.github.cocosoys.mc.mcerp.entity.SysDictType;
import com.github.cocosoys.mc.mcerp.entity.vo.DictOptionVO;
import com.github.cocosoys.mc.mcerp.service.AuthService;
import com.github.cocosoys.mc.mcerp.service.OperLogService;
import com.github.cocosoys.mc.mcerp.service.SysDictService;
import com.github.cocosoys.mc.soyshttpovermc.util.PageUtils;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.orm.DATA;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * 字典管理实现（从 SysDictController 迁入）：类型/数据 CRUD；
 * 删除类型时连带删除该类型下所有字典数据。
 */
public class SysDictServiceImpl implements SysDictService {

    private final OperLogService operLog;

    public SysDictServiceImpl(OperLogService operLog) {
        this.operLog = operLog;
    }

    // ===== 字典类型 =====

    @Override
    public TableDataInfo typeList(Integer pageNum, Integer pageSize, String dictName, String dictType) {
        List<SysDictType> all = DATA.select(SysDictType.class);
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
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult typeDetail(String dictId) {
        SysDictType dt = DATA.get(SysDictType.class, dictId);
        if (dt == null) {
            return AjaxResult.error("字典类型不存在");
        }
        return AjaxResult.success(dt);
    }

    @Override
    public AjaxResult typeAdd(SysDictType body) {
        String type = body.getDictType() == null ? "" : body.getDictType().trim();
        if (type.isEmpty()) {
            return AjaxResult.error("字典类型编码不能为空");
        }
        for (SysDictType t : DATA.select(SysDictType.class)) {
            if (type.equals(t.getDictType())) {
                return AjaxResult.error("字典类型已存在");
            }
        }
        SysDictType dt = new SysDictType();
        dt.setDictId(UUID.randomUUID().toString());
        dt.setDictName(body.getDictName() == null || body.getDictName().isEmpty() ? type : body.getDictName());
        dt.setDictType(type);
        dt.setStatus(body.getStatus() == null || body.getStatus().isEmpty() ? "0" : body.getStatus());
        dt.setRemark(body.getRemark() == null ? "" : body.getRemark());
        dt.setCreateTime(AuthService.now());
        DATA.insert(dt);
        operLog.record("字典管理", "新增字典类型", type, "新增成功");
        return AjaxResult.success("新增成功");
    }

    @Override
    public AjaxResult typeUpdate(String dictId, SysDictType body) {
        SysDictType dt = dictId == null || dictId.isEmpty() ? null : DATA.get(SysDictType.class, dictId);
        if (dt == null) {
            return AjaxResult.error("字典类型不存在");
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
        operLog.record("字典管理", "修改字典类型", dt.getDictType(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult typeRemove(String dictIds) {
        if (dictIds == null || dictIds.isEmpty()) {
            return AjaxResult.error("缺少 dictId");
        }
        for (String id : dictIds.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysDictType dt = DATA.get(SysDictType.class, id.trim());
            DATA.deleteById(SysDictType.class, id.trim());
            if (dt != null) {
                // 连带删除该类型下的字典数据
                for (SysDictData d : DATA.select(SysDictData.class)) {
                    if (dt.getDictType().equals(d.getDictType())) {
                        DATA.deleteById(SysDictData.class, d.getDictCode());
                    }
                }
                operLog.record("字典管理", "删除字典类型", dt.getDictType(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    @Override
    public AjaxResult optionselect() {
        return AjaxResult.success(DATA.select(SysDictType.class));
    }

    @Override
    public AjaxResult refreshCache() {
        return AjaxResult.success("刷新成功");
    }

    // ===== 字典数据 =====

    @Override
    public TableDataInfo dataList(Integer pageNum, Integer pageSize, String dictType, String dictLabel) {
        List<SysDictData> all = DATA.select(SysDictData.class);
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
        return PageUtils.page(filtered, pageNum, pageSize);
    }

    @Override
    public AjaxResult dataByType(String dictType) {
        List<DictOptionVO> out = new ArrayList<>();
        for (SysDictData d : DATA.select(SysDictData.class)) {
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
        SysDictData d = DATA.get(SysDictData.class, dictCode);
        if (d == null) {
            return AjaxResult.error("字典数据不存在");
        }
        return AjaxResult.success(d);
    }

    @Override
    public AjaxResult dataAdd(SysDictData body) {
        SysDictData d = new SysDictData();
        d.setDictCode(UUID.randomUUID().toString());
        d.setDictSort(body.getDictSort());
        d.setDictLabel(body.getDictLabel() == null ? "" : body.getDictLabel());
        d.setDictValue(body.getDictValue() == null ? "" : body.getDictValue());
        d.setDictType(body.getDictType() == null ? "" : body.getDictType());
        d.setStatus(body.getStatus() == null || body.getStatus().isEmpty() ? "0" : body.getStatus());
        d.setRemark(body.getRemark() == null ? "" : body.getRemark());
        if (d.getDictType().isEmpty()) {
            return AjaxResult.error("字典类型不能为空");
        }
        DATA.insert(d);
        operLog.record("字典管理", "新增字典数据", d.getDictLabel(), "新增成功");
        return AjaxResult.success("新增成功");
    }

    @Override
    public AjaxResult dataUpdate(String dictCode, SysDictData body) {
        SysDictData d = dictCode == null || dictCode.isEmpty() ? null : DATA.get(SysDictData.class, dictCode);
        if (d == null) {
            return AjaxResult.error("字典数据不存在");
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
        operLog.record("字典管理", "修改字典数据", d.getDictLabel(), "修改成功");
        return AjaxResult.success("修改成功");
    }

    @Override
    public AjaxResult dataRemove(String dictCodes) {
        if (dictCodes == null || dictCodes.isEmpty()) {
            return AjaxResult.error("缺少 dictCode");
        }
        for (String id : dictCodes.split(",")) {
            if (id.trim().isEmpty()) {
                continue;
            }
            SysDictData d = DATA.get(SysDictData.class, id.trim());
            DATA.deleteById(SysDictData.class, id.trim());
            if (d != null) {
                operLog.record("字典管理", "删除字典数据", d.getDictLabel(), "删除成功");
            }
        }
        return AjaxResult.success("删除成功");
    }

    // ===== 内部 =====

    private static boolean contains(String source, String keyword) {
        return source != null && source.toLowerCase().contains(keyword.toLowerCase());
    }
}
