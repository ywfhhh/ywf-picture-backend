package com.ywf.ywfpicturebackend.manager.upload;

import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.ImageInfo;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.infrastructure.config.TxYunCosClientConfig;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.manager.TxYunCosManager;
import com.ywf.ywfpicturebackend.interfaces.dto.file.UploadPictureResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.io.File;

@Slf4j
@Service
public class TxYunFilePictureUpload extends FilePictureUpload {

    @Resource
    public TxYunCosManager cosManager;

    @Resource
    public TxYunCosClientConfig txYunCosClientConfig;

    /**
     * 模板方法，定义上传流程
     */
    public final UploadPictureResult uploadPicture(File file, String uploadPath, String picName) {
        try {
            PutObjectResult putObjectResult = cosManager.putPictureObject(uploadPath, file);
            ImageInfo imageInfo = putObjectResult.getCiUploadResult().getOriginalInfo().getImageInfo();
            // 5. 封装返回结果
            return buildResult(picName, file, uploadPath, imageInfo, txYunCosClientConfig.getHost());
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        } finally {
            // 6. 清理临时文件
            deleteTempFile(file);
        }
    }

}
