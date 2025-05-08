package com.ywf.ywfpicturebackend.infrastructure.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.SecureUtil;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.infrastructure.config.AliYunCosClientConfig;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.infrastructure.manager.AliYunCosManager;
import com.ywf.ywfpicturebackend.interfaces.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

@Slf4j
@Service
public class AliYunUrlPictureUpload extends UrlPictureUpload {

    @Resource
    public AliYunCosManager cosManager;

    @Resource
    public AliYunCosClientConfig aliYunCosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(File file, String uploadPath, String picName) {
        try {
            cosManager.putPictureObject(uploadPath, file);
            UploadPictureResult uploadPictureResult = cosManager.getPictureInfo(uploadPath);
            // 5. 封装返回结果
            return buildResult(picName, file, uploadPath, uploadPictureResult);
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }

    /**
     * 封装返回结果
     */
    public UploadPictureResult buildResult(String picName, File file, String
            uploadPath, UploadPictureResult uploadPictureResult) {
        int picWidth = uploadPictureResult.getPicWidth();
        int picHeight = uploadPictureResult.getPicHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(picName);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(aliYunCosClientConfig.getHost() + "/" + uploadPath);
        uploadPictureResult.setMd5(SecureUtil.md5(file));
        return uploadPictureResult;
    }

}
