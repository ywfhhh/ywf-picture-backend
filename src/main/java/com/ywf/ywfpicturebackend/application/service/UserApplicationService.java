package com.ywf.ywfpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ywf.ywfpicturebackend.infrastructure.common.DeleteRequest;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserLoginRequest;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserRegisterRequest;
import com.ywf.ywfpicturebackend.interfaces.vo.user.LoginUserVO;
import com.ywf.ywfpicturebackend.interfaces.vo.user.UserVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

/**
 * @author yiwenfeng
 * @description 针对表【user(用户)】的数据库操作Service
 * @createDate 2025-04-25 19:36:09
 */
public interface UserApplicationService {
    /**
     * 用户注册
     *
     * @return 新用户 id
     */
    long userRegister(UserRegisterRequest userRegisterRequest);

    /**
     * 用户登陆
     */
    LoginUserVO userLogin(UserLoginRequest userLoginRequest, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    LoginUserVO getLoginUserVO(User loginUser);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    long addUser(User user);

    User getById(long id);

    UserVO getUserVOById(long id);

    boolean removeById(DeleteRequest deleteRequest);

    boolean updateById(User user);

    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    List<User> listByIds(Set<Long> userIdSet);

    UserVO getUserVO(User user);

    List<UserVO> getUserVOList(List<User> userList);
}
