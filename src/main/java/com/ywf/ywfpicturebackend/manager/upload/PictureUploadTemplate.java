package com.ywf.ywfpicturebackend.manager.upload;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.crypto.SecureUtil;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.ywf.ywfpicturebackend.interfaces.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;

import java.io.File;

@Slf4j
public abstract class PictureUploadTemplate {
    /**
     * 模板方法，定义上传流程
     */
    public abstract UploadPictureResult uploadPicture(File file, String uploadPath, String picName);

    /**
     * 校验输入源（本地文件或 URL）
     */
    public abstract void validPicture(Object inputSource);

    /**
     * 获取输入源的原始文件名
     */
    public abstract String getOriginFilename(Object inputSource);

    /**
     * 处理输入源并生成本地临时文件
     */
    public abstract void processFile(Object inputSource, File file) throws Exception;

    /**
     * 删除临时文件
     */
    public void deleteTempFile(File file) {
        if (file == null) {
            return;
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            log.error("file delete error, filepath = {}", file.getAbsolutePath());
        }
    }

    /**
     * 封装返回结果
     */
    UploadPictureResult buildResult(String originFilename, File file, String uploadPath, ImageInfo imageInfo, String host) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        int picWidth = imageInfo.getWidth();
        int picHeight = imageInfo.getHeight();
        double picScale = NumberUtil.round(picWidth * 1.0 / picHeight, 2).doubleValue();
        uploadPictureResult.setPicName(FileUtil.mainName(originFilename));
        uploadPictureResult.setPicWidth(picWidth);
        uploadPictureResult.setPicHeight(picHeight);
        uploadPictureResult.setPicScale(picScale);
        uploadPictureResult.setPicFormat(imageInfo.getFormat());
        uploadPictureResult.setPicSize(FileUtil.size(file));
        uploadPictureResult.setUrl(host + "/" + uploadPath);
        uploadPictureResult.setMd5(SecureUtil.md5(file));
        uploadPictureResult.setPicColor(imageInfo.getAve());
        return uploadPictureResult;
    }
}

