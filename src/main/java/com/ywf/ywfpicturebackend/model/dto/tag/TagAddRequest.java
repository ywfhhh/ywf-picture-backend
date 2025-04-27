package com.ywf.ywfpicturebackend.model.dto.tag;

import lombok.Data;

/**
 * 新增Tag
 */
@Data
public class TagAddRequest {
    private Long id;
    private String tagName;
}
