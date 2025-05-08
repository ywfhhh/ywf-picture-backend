package com.ywf.ywfpicturebackend.domain.picture.repository;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.ywfpicturebackend.domain.picture.entity.Picture;
import org.apache.ibatis.annotations.Param;

public interface PictureRepository extends IService<Picture> {
    int updateByShardingKey(@Param("picture") Picture picture);
    int updatePictureReviewStatus(@Param("picture") Picture picture);
}
