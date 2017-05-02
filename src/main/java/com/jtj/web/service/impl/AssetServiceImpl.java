package com.jtj.web.service.impl;

import com.jtj.web.common.Constant;
import com.jtj.web.common.PageDto;
import com.jtj.web.common.ResultCode;
import com.jtj.web.common.ResultDto;
import com.jtj.web.dao.AssetDao;
import com.jtj.web.dao.AssetTypeDao;
import com.jtj.web.dto.AssetDto;
import com.jtj.web.dto.AssetTypeDto;
import com.jtj.web.entity.*;
import com.jtj.web.service.AssetService;
import com.jtj.web.service.AssetTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;

/**
 * Created by MrTT (jiang.taojie@foxmail.com)
 * 2017/3/15.
 */
@Service
public class AssetServiceImpl
        extends BaseServiceImpl<Asset,AssetDto,AssetDao>
        implements AssetService {

    @Autowired
    private AssetDao assetDao;

    @Override
    public ResultDto<Object> add(Asset t) {
        String uuid = UUID.randomUUID().toString();
        t.setUuid(uuid);
        ResultDto<Object> result = super.add(t);
        addOperationRecord(uuid, Constant.OperationType.ADD,result.getTitle());
        return result;
    }

    private int addOperationRecord(String uuid,Constant.OperationType type){
        return addOperationRecord(uuid, type, null);
    }

    private int addOperationRecord(String uuid,Constant.OperationType type,String remark){
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        HttpSession session = request.getSession();
        User user = (User) session.getAttribute(Constant.SESSION_USER);
        AssetOperationRecord record = new AssetOperationRecord();
        record.setUuid(uuid);
        record.setOperationType(type.getId());
        record.setUserId(user.getId());
        record.setRemark(remark);
        return assetDao.addOperationRecord(record);
    }

    @Override
    public ResultDto<List<AssetOperationRecord>> getOperationRecordByUuid(String uuid) {
        ResultDto<List<AssetOperationRecord>> result = new ResultDto<>(ResultCode.SUCCESS);
        result.setObject(assetDao.getOperationRecordByUuid(uuid));
        return result;
    }

    @Override
    public ResultDto<Object> borrowAsset(AssetDto dto) {
        ResultDto<Object> result = new ResultDto<>();
        List<Asset> assets = assetDao.getList(dto);
        if (assets.size() == 0){
            result.setResultCode(ResultCode.ASSET_NON_EXISTENT);
            return result;
        }
        if (assets.size() > 1){
            result.setResultCode(ResultCode.ASSET_NOT_ONLY);
            result.setMessage("请使用uuid或者添加更多条件");
            return result;
        }
        //todo 权限判断
        result.setResultCode(updateAssetStatus(assets.get(0).getUuid(), Constant.AssetStatus.BORROW) == 1?
                ResultCode.SUCCESS:ResultCode.OPERATE_FAIL);
        addOperationRecord(assets.get(0).getUuid(), Constant.OperationType.BORROW,result.getTitle());
        return result;
    }

    @Override
    public ResultDto<Object> returnAsset(AssetDto dto) {
        ResultDto<Object> result = new ResultDto<>();
        List<Asset> assets = assetDao.getList(dto);
        if (assets.size() == 0){
            result.setResultCode(ResultCode.ASSET_NON_EXISTENT);
            return result;
        }
        if (assets.size() > 1){
            result.setResultCode(ResultCode.ASSET_NOT_ONLY);
            result.setMessage("请使用uuid或者添加更多条件");
            return result;
        }
        result.setResultCode(updateAssetStatus(assets.get(0).getUuid(), Constant.AssetStatus.NORMAL) == 1?
                ResultCode.SUCCESS:ResultCode.OPERATE_FAIL);
        addOperationRecord(assets.get(0).getUuid(), Constant.OperationType.RETURN,result.getTitle());
        return result;
    }

    private int updateAssetStatus(String uuid, Constant.AssetStatus status){
        return assetDao.updateAssetStatus(uuid,status.getId());
    }
}