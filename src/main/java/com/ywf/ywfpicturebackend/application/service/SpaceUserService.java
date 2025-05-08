package com.ywf.ywfpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.ywfpicturebackend.interfaces.dto.spaceuser.SpaceUserAddRequest;
import com.ywf.ywfpicturebackend.interfaces.dto.spaceuser.SpaceUserQueryRequest;
import com.ywf.ywfpicturebackend.domain.user.entity.SpaceUser;
import com.ywf.ywfpicturebackend.interfaces.vo.SpaceUserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author yiwenfeng
 * @description 针对表【space_user(空间用户关联)】的数据库操作Service
 * @createDate 2025-04-30 09:40:33
 */
public interface SpaceUserService extends IService<SpaceUser> {
    void validSpaceUser(SpaceUser spaceUser, boolean add);

    long addSpaceUser(SpaceUserAddRequest spaceUserAddRequest);

    QueryWrapper<SpaceUser> getQueryWrapper(SpaceUserQueryRequest spaceUserQueryRequest);

    SpaceUserVO getSpaceUserVO(SpaceUser spaceUser, HttpServletRequest request);

    List<SpaceUserVO> getSpaceUserVOList(List<SpaceUser> spaceUserList);
}
