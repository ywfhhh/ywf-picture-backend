package com.ywf.ywfpicturebackend.application.service.impl;

import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.application.service.UserAppliactionService;
import com.ywf.ywfpicturebackend.application.service.UserApplicationService;
import com.ywf.ywfpicturebackend.auth.StpKit;
import com.ywf.ywfpicturebackend.domain.user.repository.UserRepository;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.domain.user.constant.UserConstant;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.infrastructure.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.infrastructure.mapper.UserMapper;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserQueryRequest;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.domain.user.valueobj.UserRoleEnum;
import com.ywf.ywfpicturebackend.interfaces.dto.user.UserRegisterRequest;
import com.ywf.ywfpicturebackend.interfaces.vo.LoginUserVO;
import com.ywf.ywfpicturebackend.application.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

/**
 * @author yiwenfeng
 * @description 针对表【user(用户)】的数据库操作Service实现
 * @createDate 2025-04-25 19:36:09
 */
@Service
public class UserApplicationServiceImpl extends ServiceImpl<UserMapper, User>
        implements UserApplicationService {
    @Resource
    UserDomain userRepository;

    @Override
    public long userRegister(UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        User.validUserRegister(userAccount, userPassword, checkPassword);
        return user
    }

    public String getEncryptPassword(String userPassword) {
        // 盐值，混淆密码
        final String SALT = "ywf";
        return DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
    }

    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        // 从数据库查询（追求性能的话可以注释，直接返回上述结果）
        long userId = currentUser.getId();
        currentUser = this.getById(userId);
        if (currentUser == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return currentUser;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public boolean userLogout(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (userObj == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        User loginUser = (User) userObj;
        // 移除登录态
        request.getSession().removeAttribute(UserConstant.USER_LOGIN_STATE);
        StpKit.SPACE.logout(loginUser.getId());
        return true;
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }
        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        String userName = userQueryRequest.getUserName();
        String userProfile = userQueryRequest.getUserProfile();
        String userRole = userQueryRequest.getUserRole();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(ObjUtil.isNotNull(id), "id", id);
        queryWrapper.eq(StrUtil.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.like(StrUtil.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.like(StrUtil.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StrUtil.isNotBlank(userProfile), "userProfile", userProfile);
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

}




