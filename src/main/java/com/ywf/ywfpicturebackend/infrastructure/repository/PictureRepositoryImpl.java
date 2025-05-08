package com.ywf.ywfpicturebackend.infrastructure.repository;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.domain.picture.repository.PictureRepository;
import com.ywf.ywfpicturebackend.domain.picture.entity.Picture;
import com.ywf.ywfpicturebackend.infrastructure.mapper.PictureMapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Service;

@Service
public class PictureRepositoryImpl extends ServiceImpl<PictureMapper, Picture> implements PictureRepository {

    @Override
    public int updateByShardingKey(Picture picture) {
        return this.updatePictureReviewStatus(picture);
    }

    @Override
    public int updatePictureReviewStatus(Picture picture) {
        return this.updateByShardingKey(picture);
    }
}

