package com.ywf.ywfpicturebackend.model.dto.picture;

import lombok.Data;

import java.io.Serializable;

@Data
public class SearchPictureByPictureRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;
    /**
     * 图片所属图库id
     */
    private Long spaceId;
    private static final long serialVersionUID = 1L;
}

