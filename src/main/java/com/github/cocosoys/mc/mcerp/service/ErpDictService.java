package com.github.cocosoys.mc.mcerp.service;

import com.github.cocosoys.mc.mcerp.entity.ErpDictData;
import com.github.cocosoys.mc.mcerp.entity.ErpDictType;
import com.github.cocosoys.mc.soyshttpovermc.util.TableDataInfo;
import com.github.cocosoys.mc.soyshttpovermc.util.AjaxResult;
import com.github.cocosoys.mc.soyshttpovermc.web.gateway.policy.auth.issuer.CredentialPresentation;

/**
 * 字典管理服务（抽象契约）：字典类型 + 字典数据 CRUD。
 * 实现见 {@link com.github.cocosoys.mc.mcerp.impl.ErpDictServiceImpl}；
 * 删除类型连带删除该类型数据等一致性编排由 impl 负责。
 */
public interface ErpDictService {

    TableDataInfo typeList(Integer pageNum, Integer pageSize, String dictName, String dictType);

    AjaxResult typeDetail(String dictId);

    AjaxResult typeAdd(ErpDictType body, CredentialPresentation credential);

    AjaxResult typeUpdate(String dictId, ErpDictType body, CredentialPresentation credential);

    AjaxResult typeRemove(String dictIds, CredentialPresentation credential);

    AjaxResult optionselect();

    AjaxResult refreshCache();

    TableDataInfo dataList(Integer pageNum, Integer pageSize, String dictType, String dictLabel);

    AjaxResult dataByType(String dictType);

    AjaxResult dataDetail(String dictCode);

    AjaxResult dataAdd(ErpDictData body, CredentialPresentation credential);

    AjaxResult dataUpdate(String dictCode, ErpDictData body, CredentialPresentation credential);

    AjaxResult dataRemove(String dictCodes, CredentialPresentation credential);
}
