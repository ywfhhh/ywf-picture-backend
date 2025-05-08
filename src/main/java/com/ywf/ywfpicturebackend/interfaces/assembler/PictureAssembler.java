package com.ywf.ywfpicturebackend.interfaces.assembler;

import cn.hutool.json.JSONUtil;
import com.ywf.ywfpicturebackend.domain.picture.entity.Picture;
import com.ywf.ywfpicturebackend.interfaces.dto.picture.PictureEditRequest;
import com.ywf.ywfpicturebackend.interfaces.dto.picture.PictureUpdateRequest;
import org.springframework.beans.BeanUtils;

public class PictureAssembler {

    public static Picture toPictureEntity(PictureEditRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        return picture;
    }

    public static Picture toPictureEntity(PictureUpdateRequest request) {
        Picture picture = new Picture();
        BeanUtils.copyProperties(request, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(request.getTags()));
        return picture;
    }
}

