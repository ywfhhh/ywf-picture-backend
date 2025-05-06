package com.ywf.ywfpicturebackend.config;

import com.aliyun.oss.ClientBuilderConfiguration;
import com.aliyun.oss.OSS;
import com.aliyun.oss.OSSClientBuilder;
import com.aliyun.oss.common.auth.CredentialsProvider;
import com.aliyun.oss.common.auth.DefaultCredentialProvider;
import com.aliyun.oss.common.comm.SignVersion;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "aliyun-cos.client")
@Data
public class AliYunCosClientConfig {

    /**
     * 域名
     */
    private String endpoint;

    /**
     * secretId
     */
    private String accessKeyId;

    /**
     * 密钥（注意不要泄露）
     */
    private String accessKeySecret;

    /**
     * region
     *
     * @return
     */
    private String region;
    /**
     * bucketName
     */
    private String bucketName;

    /**
     * host
     *
     * @return
     */
    private String host;

    @Bean
    public OSS aliYunCosClient() {
        // 使用DefaultCredentialProvider方法直接设置AK和SK
        CredentialsProvider credentialsProvider = new DefaultCredentialProvider(accessKeyId, accessKeySecret);
        // 使用credentialsProvider初始化客户端
        ClientBuilderConfiguration clientBuilderConfiguration = new ClientBuilderConfiguration();
        // 显式声明使用 V4 签名算法
        clientBuilderConfiguration.setSignatureVersion(SignVersion.V4);
        // 创建OSSClient实例。
        return OSSClientBuilder.create()
                .endpoint(endpoint)
                .credentialsProvider(credentialsProvider)
                .clientConfiguration(clientBuilderConfiguration)
                .region(region)
                .build();
    }
}
