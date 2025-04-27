package com.ywf.ywfpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.ywfpicturebackend.model.dto.picture.PictureQueryRequest;
import com.ywf.ywfpicturebackend.model.dto.tag.TagQueryRequest;
import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.model.entity.Tag;
import com.ywf.ywfpicturebackend.model.vo.TagVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
* @author yiwenfeng
* @description 针对表【tag(标签表)】的数据库操作Service
* @createDate 2025-04-27 01:50:56
*/
public interface TagService extends IService<Tag> {

    QueryWrapper<Tag> getQueryWrapper(TagQueryRequest tagQueryRequest);

    Page<TagVO> getTagVOPage(Page<Tag> picturePage);
}
