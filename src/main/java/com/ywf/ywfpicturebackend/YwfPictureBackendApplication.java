package com.ywf.ywfpicturebackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@MapperScan("com.ywf.ywfpicturebackend.mapper")
@EnableAspectJAutoProxy(proxyTargetClass = true)// 可以通过AopContext对象获取代理对象
public class YwfPictureBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(YwfPictureBackendApplication.class, args);
    }

}
