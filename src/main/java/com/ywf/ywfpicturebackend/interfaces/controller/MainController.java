package com.ywf.ywfpicturebackend.interfaces.controller;

import com.ywf.ywfpicturebackend.infrastructure.common.BaseResponse;
import com.ywf.ywfpicturebackend.infrastructure.common.ResultUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/main")
public class MainController {
    @GetMapping("/health")
    public BaseResponse<String> health() {
        return ResultUtils.success("ok");
    }
}
