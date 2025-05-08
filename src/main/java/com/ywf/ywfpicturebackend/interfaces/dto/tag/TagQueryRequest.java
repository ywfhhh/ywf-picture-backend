package com.ywf.ywfpicturebackend.interfaces.dto.tag;

import com.ywf.ywfpicturebackend.infrastructure.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

@EqualsAndHashCode(callSuper = true)
@Data
public class TagQueryRequest extends PageRequest implements Serializable {
    /**
     * id
     */
    private Long id;

    /**
     * 图片名称
     */
    private String tagName;

    /**
     * 创建者Id
     */
    private Long userId;
}
