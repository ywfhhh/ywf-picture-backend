package com.ywf.ywfpicturebackend.interfaces.dto.picture;

import lombok.Data;

@Data
public class PictureDeleteRequest {
    /**
     * id
     */
    private Long id;
    /**
     * 所属图库id
     */
    private Long spaceId;
    private static final long serialVersionUID = 1L;
}
