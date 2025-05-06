package com.ywf.ywfpicturebackend.manager;

import com.aliyun.oss.OSS;
import com.aliyun.oss.model.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ywf.ywfpicturebackend.config.AliYunCosClientConfig;
import com.ywf.ywfpicturebackend.model.dto.file.UploadPictureResult;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.*;

@Component
public class AliYunCosManager {

    @Resource
    private AliYunCosClientConfig aliYunCosClientConfig;

    @Resource
    private OSS aliYunCosClient;

    /**
     * 上传对象
     *
     * @param objectName 唯一键 即文件路径
     * @param file       文件
     */
    public PutObjectResult putObject(String objectName, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(aliYunCosClientConfig.getBucketName(), objectName,
                file);
        return aliYunCosClient.putObject(putObjectRequest);
    }

    /**
     * 下载对象
     *
     * @param objectName 唯一键  即文件路径
     */
    public OSSObject getObject(String objectName) {
        GetObjectRequest getObjectRequest = new GetObjectRequest(aliYunCosClientConfig.getBucketName(), objectName);
        return aliYunCosClient.getObject(getObjectRequest);
    }

    /**
     * 上传对象（附带图片信息）
     *
     * @param ObjectName 唯一键
     * @param file       文件
     */
    public PutObjectResult putPictureObject(String ObjectName, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(aliYunCosClientConfig.getBucketName(), ObjectName,
                file);
        return aliYunCosClient.putObject(putObjectRequest);
    }

    /**
     * 获取图片基本信息
     */
    public UploadPictureResult getPictureInfo(String objectName) {
        // 4. 构造请求并添加图片信息操作
        GetObjectRequest req = new GetObjectRequest(aliYunCosClientConfig.getBucketName(), objectName);
        req.setProcess("image/info");
        // 5. 执行请求，返回的是一个 OSSObject（其 content 流里是一段 JSON）
        OSSObject obj = aliYunCosClient.getObject(req);
        UploadPictureResult uploadPictureResult = new UploadPictureResult();
        try (InputStream in = obj.getObjectContent()) {
            ObjectMapper objectMapper = new ObjectMapper();
            JsonNode jsonNode = objectMapper.readTree(in);
            uploadPictureResult.setPicFormat(jsonNode.get("Format").get("value").asText());
            uploadPictureResult.setPicHeight(jsonNode.get("ImageHeight").get("value").asInt());
            uploadPictureResult.setPicWidth(jsonNode.get("ImageWidth").get("value").asInt());
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return uploadPictureResult;
    }

    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) {
        aliYunCosClient.deleteObject(aliYunCosClientConfig.getBucketName(), key);
    }

}

