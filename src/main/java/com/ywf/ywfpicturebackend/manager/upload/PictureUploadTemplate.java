package com.ywf.ywfpicturebackend.manager.upload;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.aliyun.oss.model.ObjectMetadata;
import com.aliyun.oss.model.PutObjectRequest;
import com.aliyun.oss.model.PutObjectResult;
import com.ywf.ywfpicturebackend.common.ErrorCode;
import com.ywf.ywfpicturebackend.config.AliYunCosClientConfig;
import com.ywf.ywfpicturebackend.exception.BusinessException;
import com.ywf.ywfpicturebackend.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.manager.CosManager;
import com.ywf.ywfpicturebackend.model.dto.file.UploadPictureResult;
import com.ywf.ywfpicturebackend.model.dto.picture.PictureUploadRequest;
import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.model.entity.User;
import com.ywf.ywfpicturebackend.model.vo.PictureVO;
import com.ywf.ywfpicturebackend.service.PictureService;
import com.ywf.ywfpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Resource;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
public abstract class PictureUploadTemplate {

    @Resource
    public CosManager cosManager;

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
     * 封装返回结果
     */
    private UploadPictureResult buildResult(String picName, File file, String
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

    /**
     * 封装返回结果
     */
    private UploadPictureResult buildResult(Picture picture, String originFileName, String uploadPath) {
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        uploadPictureResult.setPicName(FileUtil.mainName(originFileName));
        uploadPictureResult.setPicScale(picture.getPicScale());
        uploadPictureResult.setPicSize(picture.getPicSize());
        uploadPictureResult.setUrl(picture.getUrl());
        uploadPictureResult.setPicScale(picture.getPicScale());
        uploadPictureResult.setPicWidth(picture.getPicWidth());
        uploadPictureResult.setPicHeight(picture.getPicHeight());
        return uploadPictureResult;
    }

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
}

