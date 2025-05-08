package com.ywf.ywfpicturebackend.auth;

import cn.dev33.satoken.stp.StpInterface;
import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.ReflectUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.ContentType;
import cn.hutool.http.Header;
import cn.hutool.json.JSONUtil;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.domain.user.constant.UserConstant;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.domain.user.entity.Picture;
import com.ywf.ywfpicturebackend.domain.user.entity.Space;
import com.ywf.ywfpicturebackend.domain.user.entity.SpaceUser;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.domain.user.valueobj.SpaceRoleEnum;
import com.ywf.ywfpicturebackend.domain.user.valueobj.SpaceTypeEnum;
import com.ywf.ywfpicturebackend.domain.user.valueobj.UserRoleEnum;
import com.ywf.ywfpicturebackend.application.service.PictureService;
import com.ywf.ywfpicturebackend.application.service.SpaceService;
import com.ywf.ywfpicturebackend.application.service.SpaceUserService;
import com.ywf.ywfpicturebackend.application.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;

/**
 * 自定义权限加载接口实现类
 */
@Component    // 保证此类被 SpringBoot 扫描，完成 Sa-Token 的自定义权限验证扩展
public class StpInterfaceImpl implements StpInterface {
    @Resource
    SpaceUserService spaceUserService;
    @Resource
    SpaceUserAuthManager spaceUserAuthManager;
    @Resource
    SpaceAuthManager spaceAuthManager;
    @Resource
    PictureService pictureService;
    @Resource
    UserService userService;
    @Resource
    SpaceService spaceService;
    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private UserAuthManager userAuthManager;

    /**
     * 从请求中获取上下文对象
     */
    private SpaceUserAuthContext getAuthContextByRequest() {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
        String contentType = request.getHeader(Header.CONTENT_TYPE.getValue());
        SpaceUserAuthContext authRequest;
        // 兼容 get 和 post 操作
        if (ContentType.JSON.getValue().equals(contentType)) {
            String body = ServletUtil.getBody(request);
            authRequest = JSONUtil.toBean(body, SpaceUserAuthContext.class);
        } else {
            Map<String, String> paramMap = ServletUtil.getParamMap(request);
            authRequest = BeanUtil.toBean(paramMap, SpaceUserAuthContext.class);
        }
        // 根据请求路径区分 id 字段的含义
        Long id = authRequest.getId();
        if (ObjUtil.isNotNull(id)) {
            String requestUri = request.getRequestURI();
            String partUri = requestUri.replace(contextPath + "/", "");
            String moduleName = StrUtil.subBefore(partUri, "/", false);
            switch (moduleName) {
                case "picture":
                    authRequest.setPictureId(id);
                    break;
                case "spaceUser":
                    authRequest.setSpaceUserId(id);
                    break;
                case "space":
                    authRequest.setSpaceId(id);
                    break;
                default:
            }
        }
        return authRequest;
    }

    private boolean isAllFieldsNull(Object object) {
        if (object == null) {
            return true; // 对象本身为空
        }
        // 获取所有字段并判断是否所有字段都为空
        return Arrays.stream(ReflectUtil.getFields(object.getClass()))
                // 获取字段值
                .map(field -> ReflectUtil.getFieldValue(object, field))
                // 检查是否所有字段都为空
                .allMatch(ObjectUtil::isEmpty);
    }

    /**
     * 返回一个账号所拥有的权限码集合
     * 1.用户管理图片权限
     * 2.用户空间管理权限
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 判断 loginType，仅对类型为 "space" 进行权限校验
        if (!StpKit.SPACE_TYPE.equals(loginType)) {
            return new ArrayList<>();
        }
        // 获取 userId
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        List<String> permissionList = new ArrayList<>();
        // 添加用户角色基本权限
        permissionList.addAll(userAuthManager.getPermissionList(loginUser));
        // 添加用户角色基本空间权限
        permissionList.addAll(spaceAuthManager.getPermissionList(loginUser));
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            // 依据空间角色获取对团队空间的权限
            permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole()));
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            // 这里的spaceUserId指的是要修改的SpaceUser表id
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceUser.getSpaceId()).eq(SpaceUser::getUserId, loginUser.getId()).one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(loginSpaceUser.getSpaceRole()));
        }
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null || spaceId == 0L) {
            if (loginUser.getUserRole() == UserRoleEnum.ADMIN.getValue())
                permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
            else
                // 普通用户不能够对公共图库进行删除
                permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.EDITOR.getValue()));
            spaceId = 0L;
        } else {
            // 要么是私有要么是团队
            // 获取 Space 对象
            Space space = spaceService.getById(spaceId);
            if (space == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
            }
            // 根据 Space 类型判断权限
            if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
                // 私有空间，仅本人或管理员有权限
                if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
                } else {
                    return permissionList;
                }
            } else {
                // 团队空间，查询 SpaceUser 并获取角色和权限
                spaceUser = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceId).eq(SpaceUser::getUserId, loginUser.getId()).one();
                if (spaceUser == null) {
                    return new ArrayList<>();
                }
                permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole()));
            }
        }
        // 查看用户对某张图片的权限
        Long pictureId = authContext.getPictureId();
        if (pictureId != null) {
            Picture picture = pictureService.lambdaQuery().eq(Picture::getId, pictureId).eq(Picture::getSpaceId, spaceId).select(Picture::getId, Picture::getSpaceId, Picture::getUserId).one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            if (picture.getUserId().equals(loginUser.getId()) || loginUser.getUserRole() == UserRoleEnum.ADMIN.getValue()) {
                permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
                return permissionList;
            }
            spaceId = picture.getSpaceId();
            SpaceUser spaceUser1 = spaceUserService.lambdaQuery().eq(SpaceUser::getSpaceId, spaceId).eq(SpaceUser::getUserId, loginUser.getId()).one();
            if (spaceUser1 != null) {
                permissionList.addAll(spaceUserAuthManager.getPermissionsByRole(spaceUser1.getSpaceRole()));
                return permissionList;
            }
            if (spaceId == 0L)
                permissionList.addAll(spaceAuthManager.getPermissionsByRole(SpaceRoleEnum.VIEWER.getValue()));
            return permissionList;
        }
        return permissionList;
    }


    /**
     * 返回一个账号所拥有的角色标识集合 (权限与角色可分开校验)
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 本 list 仅做模拟，实际项目中要根据具体业务逻辑来查询角色
        User loginUser = (User) StpKit.SPACE.getSessionByLoginId(loginId).get(UserConstant.USER_LOGIN_STATE);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "用户未登录");
        }
        String userRole = loginUser.getUserRole();
        return Arrays.asList(userRole.split(","));
    }
}

