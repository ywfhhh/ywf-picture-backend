package com.ywf.ywfpicturebackend.interfaces.vo.tag;

import lombok.Data;

import java.util.Date;

@Data
public class TagVO {
    /**
     * id
     */
    private Long id;

    private String tagName;

    private String userName;
    /**
     * 创建时间
     */
    private Date createTime;
}
