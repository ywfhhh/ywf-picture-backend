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
import com.ywf.ywfpicturebackend.common.ErrorCode;
import com.ywf.ywfpicturebackend.constant.PicturePermissionConstant;
import com.ywf.ywfpicturebackend.constant.SpaceUserPermissionConstant;
import com.ywf.ywfpicturebackend.constant.UserConstant;
import com.ywf.ywfpicturebackend.exception.BusinessException;
import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.model.entity.Space;
import com.ywf.ywfpicturebackend.model.entity.SpaceUser;
import com.ywf.ywfpicturebackend.model.entity.User;
import com.ywf.ywfpicturebackend.model.enums.SpaceRoleEnum;
import com.ywf.ywfpicturebackend.model.enums.SpaceTypeEnum;
import com.ywf.ywfpicturebackend.model.enums.UserRoleEnum;
import com.ywf.ywfpicturebackend.service.PictureService;
import com.ywf.ywfpicturebackend.service.SpaceService;
import com.ywf.ywfpicturebackend.service.SpaceUserService;
import com.ywf.ywfpicturebackend.service.UserService;
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
        List<String> USER_ALL_PERMISSIONS = userAuthManager.getPermissionsByRole(loginUser.getUserRole());
        // 获取上下文对象
        SpaceUserAuthContext authContext = getAuthContextByRequest();
        // 如果所有字段都为空，查公共图库
        if (isAllFieldsNull(authContext)) {
            USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
            return USER_ALL_PERMISSIONS;
        }
        // 优先从上下文中获取 SpaceUser 对象
        SpaceUser spaceUser = authContext.getSpaceUser();
        if (spaceUser != null) {
            USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole()));
            return USER_ALL_PERMISSIONS;
        }
        // 如果有 spaceUserId，必然是团队空间，通过数据库查询 SpaceUser 对象
        Long spaceUserId = authContext.getSpaceUserId();
        if (spaceUserId != null) {
            spaceUser = spaceUserService.getById(spaceUserId);
            if (spaceUser == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间用户信息");
            }
            // 取出当前登录用户对应的 spaceUser
            SpaceUser loginSpaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceUser.getSpaceId())
                    .eq(SpaceUser::getUserId, loginUser.getId())
                    .one();
            if (loginSpaceUser == null) {
                return new ArrayList<>();
            }
            // 这里会导致管理员在私有空间没有权限，可以再查一次库处理
            USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(spaceUser.getSpaceRole()));
            return USER_ALL_PERMISSIONS;
        }
        // 如果没有 spaceUserId，尝试通过 spaceId 或 pictureId 获取 Space 对象并处理
        Long spaceId = authContext.getSpaceId();
        if (spaceId == null) {
            // 如果spaceId==0，通过 pictureId 获取 Picture 对象和 Space 对象
            Long pictureId = authContext.getPictureId();
            // 图片 id 也没有，则默认通过权限校验
            if (pictureId == null) {
                USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
                return USER_ALL_PERMISSIONS;
            }
            Picture picture = pictureService.lambdaQuery()
                    .eq(Picture::getId, pictureId)
                    .select(Picture::getId, Picture::getSpaceId, Picture::getUserId)
                    .one();
            if (picture == null) {
                throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到图片信息");
            }
            spaceId = picture.getSpaceId();
            // 公共图库，仅本人或管理员可操作
            if (spaceId == 0) {
                if (picture.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                    USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
                    return USER_ALL_PERMISSIONS;
                } else {
                    // 不是自己的图片，仅可查看
                    USER_ALL_PERMISSIONS.addAll(Collections.singletonList(SpaceUserPermissionConstant.PICTURE_VIEW));
                    return USER_ALL_PERMISSIONS;
                }
            }
        }
        // 获取 Space 对象
        Space space = spaceService.getById(spaceId);
        if (space == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "未找到空间信息");
        }
        // 根据 Space 类型判断权限
        if (space.getSpaceType() == SpaceTypeEnum.PRIVATE.getValue()) {
            // 私有空间，仅本人或管理员有权限
            if (space.getUserId().equals(loginUser.getId()) || userService.isAdmin(loginUser)) {
                USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
                return USER_ALL_PERMISSIONS;
            } else {
                return new ArrayList<>();
            }
        } else {
            // 团队空间，查询 SpaceUser 并获取角色和权限
            spaceUser = spaceUserService.lambdaQuery()
                    .eq(SpaceUser::getSpaceId, spaceId)
                    .eq(SpaceUser::getUserId, loginUser.getId())
                    .one();
            if (spaceUser == null) {
                return new ArrayList<>();
            }
            USER_ALL_PERMISSIONS.addAll(spaceUserAuthManager.getPermissionsByRole(SpaceRoleEnum.ADMIN.getValue()));
            return USER_ALL_PERMISSIONS;
        }
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

