package com.ywf.ywfpicturebackend.controller;

import com.ywf.ywfpicturebackend.common.BaseResponse;
import com.ywf.ywfpicturebackend.common.ResultUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class MainController {
    @RequestMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
