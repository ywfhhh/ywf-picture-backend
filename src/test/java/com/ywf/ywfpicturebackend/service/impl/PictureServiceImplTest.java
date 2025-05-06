package com.ywf.ywfpicturebackend.service.impl;

import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.service.PictureService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;

import static org.junit.jupiter.api.Assertions.*;

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