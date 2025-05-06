package com.ywf.ywfpicturebackend.model.entity.auth;

import com.ywf.ywfpicturebackend.model.entity.CommonPermission;
import com.ywf.ywfpicturebackend.model.entity.CommonRole;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SpaceAuthConfig implements Serializable {
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
