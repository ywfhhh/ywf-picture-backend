package com.ywf.ywfpicturebackend.service;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.ywfpicturebackend.model.dto.space.SpaceAddRequest;
import com.ywf.ywfpicturebackend.model.dto.space.SpaceQueryRequest;
import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.model.entity.Space;
import com.ywf.ywfpicturebackend.model.entity.User;
import com.ywf.ywfpicturebackend.model.vo.PictureVO;
import com.ywf.ywfpicturebackend.model.vo.SpaceVO;

import javax.servlet.http.HttpServletRequest;

/**
 * @author yiwenfeng
 * @description 针对表【space(空间)】的数据库操作Service
 * @createDate 2025-04-29 16:50:16
 */
public interface SpaceService extends IService<Space> {
    void validSpace(Space space, boolean add);

    void fillSpaceBySpaceLevel(Space space);

    Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser);

    Wrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest);

    Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request);

    SpaceVO getSpaceVO(Space space, HttpServletRequest request);
}
