package com.ywf.ywfpicturebackend.interfaces.dto.picture;

import com.ywf.ywfpicturebackend.api.aliyunai.CreateOutPaintingTaskRequest;
import lombok.Data;

import java.io.Serializable;

@Data
public class CreatePictureOutPaintingTaskRequest implements Serializable {

    /**
     * 图片 id
     */
    private Long pictureId;
    /**
     * 图片 id
     */
    private Long spaceId;
    /**
     * 扩图参数
     */
    private CreateOutPaintingTaskRequest.Parameters parameters;

    private static final long serialVersionUID = 1L;
}

