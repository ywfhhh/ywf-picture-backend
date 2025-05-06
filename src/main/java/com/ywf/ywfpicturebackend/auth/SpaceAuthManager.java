package com.ywf.ywfpicturebackend.auth;

import cn.hutool.core.io.resource.ResourceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.ywf.ywfpicturebackend.model.entity.*;
import com.ywf.ywfpicturebackend.model.entity.auth.SpaceAuthConfig;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SpaceAuthManager {

    public static final SpaceAuthConfig Space_AUTH_CONFIG;

    static {
        String json = ResourceUtil.readUtf8Str("biz/SpaceAuthConfig.json");
        Space_AUTH_CONFIG = JSONUtil.toBean(json, SpaceAuthConfig.class);
    }

    /**
     * 根据角色获取图片权限列表
     */
    public List<String> getPermissionsByRole(String role) {
        if (StrUtil.isBlank(role)) {
            return new ArrayList<>();
        }
        // 找到匹配的角色
        CommonRole commonRole = Space_AUTH_CONFIG.getRoles().stream()
                .filter(r -> role.equals(r.getKey()))
                .findFirst()
                .orElse(null);
        if (commonRole == null) {
            return new ArrayList<>();
        }
        return commonRole.getPermissions();
    }
    // 根据xx获取权限列表
    public List<String> getPermissionList(User loginUser) {
        if (loginUser == null) {
            return new ArrayList<>();
        }
        return getPermissionsByRole(loginUser.getUserRole());
    }

}

