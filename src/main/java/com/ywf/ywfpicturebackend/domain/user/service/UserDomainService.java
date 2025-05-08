package com.ywf.ywfpicturebackend.domain.user.service;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserRegisterRequest;
import com.ywf.ywfpicturebackend.interfaces.vo.LoginUserVO;
import com.ywf.ywfpicturebackend.interfaces.vo.UserVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public interface UserDomainService extends IService<User> {
    /**
     * 用户注册
     *
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword);

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

    String getEncryptPassword(String userPassword);

    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    default UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        return userVO;
    }

    default List<UserVO> getUserVOList(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);


}
