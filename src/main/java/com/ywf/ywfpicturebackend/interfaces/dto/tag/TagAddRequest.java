package com.ywf.ywfpicturebackend.interfaces.dto.tag;

import lombok.Data;

/**
 * 新增Tag
 */
@Data
public class TagAddRequest {
    private Long id;
    private String tagName;
}
