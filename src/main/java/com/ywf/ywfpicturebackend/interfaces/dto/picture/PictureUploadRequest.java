package com.ywf.ywfpicturebackend.interfaces.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class PictureUploadRequest implements Serializable {

    /**
     * 图片 id（用于修改）
     */
    private Long id;

    /**
     * 文件地址
     */
    private String fileUrl;

    private String picName;

    private List<String> tags;

    private Long spaceId;
    private static final long serialVersionUID = 1L;
}


