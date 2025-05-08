package com.ywf.ywfpicturebackend.domain.user.entity.auth;

import com.ywf.ywfpicturebackend.domain.user.entity.CommonPermission;
import com.ywf.ywfpicturebackend.domain.user.entity.CommonRole;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceUserAuthConfig implements Serializable {

    /**
     * 权限列表
     */
    private List<CommonPermission> permissions;

    /**
     * 角色列表
     */
    private List<CommonRole> roles;

    private static final long serialVersionUID = 1L;
}

