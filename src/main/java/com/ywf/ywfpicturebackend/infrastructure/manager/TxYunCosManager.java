package com.ywf.ywfpicturebackend.infrastructure.manager;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.PutObjectResult;
import com.qcloud.cos.model.ciModel.persistence.PicOperations;
import com.ywf.ywfpicturebackend.infrastructure.config.TxYunCosClientConfig;
import com.ywf.ywfpicturebackend.infrastructure.utils.UrlParserUtils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.File;
import java.net.MalformedURLException;

@Component
public class TxYunCosManager {

    @Resource
    private TxYunCosClientConfig txYunCosClientConfig;

    @Resource
    private COSClient txYunCosClient;

    /**
     * 上传对象（附带图片信息）
     *
     * @param key  唯一键
     * @param file 文件
     */
    public PutObjectResult putPictureObject(String key, File file) {
        PutObjectRequest putObjectRequest = new PutObjectRequest(txYunCosClientConfig.getBucket(), key,
                file);
        // 对图片进行处理（获取基本信息也被视作为一种处理）
        PicOperations picOperations = new PicOperations();
        // 1 表示返回原图信息
        picOperations.setIsPicInfo(1);
        // 构造处理参数
        putObjectRequest.setPicOperations(picOperations);
        return txYunCosClient.putObject(putObjectRequest);
    }



    /**
     * 删除对象
     *
     * @param key 文件 key
     */
    public void deleteObject(String key) throws MalformedURLException {
        txYunCosClient.deleteObject(txYunCosClientConfig.getBucket(), UrlParserUtils.getKeyFromUrl(key));
    }

}

