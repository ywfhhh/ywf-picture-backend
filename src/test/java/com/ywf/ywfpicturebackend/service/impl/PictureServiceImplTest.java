package com.ywf.ywfpicturebackend.service.impl;

import com.ywf.ywfpicturebackend.domain.user.entity.Picture;
import com.ywf.ywfpicturebackend.application.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

@SpringBootTest
class PictureServiceTest {
    @Resource
    private PictureService pictureService;

    @Test
    void test() {
        Picture picture = pictureService.getById(0);
        System.out.println(picture);
    }
}