package com.ywf.ywfpicturebackend.infrastructure.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ywf.ywfpicturebackend.domain.picture.entity.Picture;
import org.apache.ibatis.annotations.Param;

/**
 * @author yiwenfeng
 * @description 针对表【picture(图片)】的数据库操作Mapper
 * @createDate 2025-04-29 09:48:42
 * @Entity generator.domain.Picture
 */
public interface PictureMapper extends BaseMapper<Picture> {
    int updateByShardingKey(@Param("picture") Picture picture);
    int updatePictureReviewStatus(@Param("picture") Picture picture);
}




