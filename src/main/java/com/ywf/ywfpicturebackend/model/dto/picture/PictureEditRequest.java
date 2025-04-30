package com.ywf.ywfpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

// 普通用户修改图片信息
@Data
public class PictureEditRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String name;

    /**
     * 简介
     */
    private String introduction;

    /**
     * 分类
     */
    private String category;

    /**
     * 标签
     */
    private List<String> tags;
    /**
     * 所属图库id
     */
    private Long spaceId;
    private static final long serialVersionUID = 1L;
}
