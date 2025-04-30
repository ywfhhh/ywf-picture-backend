package com.ywf.ywfpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.common.ErrorCode;
import com.ywf.ywfpicturebackend.exception.BusinessException;
import com.ywf.ywfpicturebackend.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.mapper.SpaceMapper;
import com.ywf.ywfpicturebackend.model.dto.space.SpaceAddRequest;
import com.ywf.ywfpicturebackend.model.dto.space.SpaceQueryRequest;
import com.ywf.ywfpicturebackend.model.dto.spaceuser.SpaceUserAddRequest;
import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.model.entity.Space;
import com.ywf.ywfpicturebackend.model.entity.SpaceUser;
import com.ywf.ywfpicturebackend.model.entity.User;
import com.ywf.ywfpicturebackend.model.enums.SpaceLevelEnum;
import com.ywf.ywfpicturebackend.model.enums.SpaceRoleEnum;
import com.ywf.ywfpicturebackend.model.enums.SpaceTypeEnum;
import com.ywf.ywfpicturebackend.model.vo.PictureVO;
import com.ywf.ywfpicturebackend.model.vo.SpaceVO;
import com.ywf.ywfpicturebackend.model.vo.UserVO;
import com.ywf.ywfpicturebackend.service.SpaceService;
import com.ywf.ywfpicturebackend.service.SpaceUserService;
import com.ywf.ywfpicturebackend.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author yiwenfeng
 * @description 针对表【space(空间)】的数据库操作Service实现
 * @createDate 2025-04-29 16:50:16
 */
@Service
public class SpaceServiceImpl extends ServiceImpl<SpaceMapper, Space>
        implements SpaceService {
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private UserService userService;
    @Resource
    private SpaceUserService spaceUserService;
    private ConcurrentHashMap<Long, Object> locks = new ConcurrentHashMap<>();

    @Override
    public void validSpace(Space space, boolean add) {
        ThrowUtils.throwIf(space == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        String spaceName = space.getSpaceName();
        Integer spaceLevel = space.getSpaceLevel();
        Integer spaceType = space.getSpaceType();
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(spaceLevel);
        // 要创建
        if (add) {
            if (StrUtil.isBlank(spaceName))
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称不能为空");
            if (spaceLevel == null)
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不能为空");
            if (spaceType == null)
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间类型不存在");
        }
        // 修改数据时，如果要改空间级别
        if (spaceLevel != null && spaceLevelEnum == null)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间级别不存在");
        if (StrUtil.isNotBlank(spaceName) && spaceName.length() > 30)
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间名称过长");
    }

    @Override
    public void fillSpaceBySpaceLevel(Space space) {
        // 根据空间级别，自动填充限额
        SpaceLevelEnum spaceLevelEnum = SpaceLevelEnum.getEnumByValue(space.getSpaceLevel());
        if (spaceLevelEnum != null) {
            long maxSize = spaceLevelEnum.getMaxSize();
            if (space.getMaxSize() == null) {
                space.setMaxSize(maxSize);
            }
            long maxCount = spaceLevelEnum.getMaxCount();
            if (space.getMaxCount() == null) {
                space.setMaxCount(maxCount);
            }
        }
    }


    @Override
    public Long addSpace(SpaceAddRequest spaceAddRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        // 默认值
        if (StrUtil.isBlank(spaceAddRequest.getSpaceName())) {
            spaceAddRequest.setSpaceName("默认空间");
        }
        if (spaceAddRequest.getSpaceLevel() == null) {
            spaceAddRequest.setSpaceLevel(SpaceLevelEnum.COMMON.getValue());
        }
        if (spaceAddRequest.getSpaceType() == null) {
            spaceAddRequest.setSpaceType(SpaceTypeEnum.PRIVATE.getValue());
        }
        // 在此处将实体类和 DTO 进行转换
        Space space = new Space();
        BeanUtils.copyProperties(spaceAddRequest, space);
        // 填充数据
        this.fillSpaceBySpaceLevel(space);
        // 数据校验
        this.validSpace(space, true);
        Integer spaceType = space.getSpaceType();
        Long userId = loginUser.getId();
        space.setUserId(userId);
        // 权限校验
        if (SpaceLevelEnum.COMMON.getValue() != spaceAddRequest.getSpaceLevel() && !userService.isAdmin(loginUser)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限创建指定级别的空间");
        }
        // 针对用户进行加锁，使用本地锁
        Object lock = locks.computeIfAbsent(userId, key -> new Object());
        synchronized (lock) {
            try {
                Long newSpaceId = transactionTemplate.execute(status -> {
                    boolean exists = this.lambdaQuery().eq(Space::getUserId, userId).eq(Space::getSpaceType, spaceType).exists();
                    ThrowUtils.throwIf(exists, ErrorCode.OPERATION_ERROR, "每个用户仅能有一个私有空间");
                    // 写入数据库
                    boolean result = this.save(space);
                    ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
                    if (spaceAddRequest.getSpaceType() == SpaceTypeEnum.TEAM.getValue()) {
                        // 将创建者加入空间作为空间管理员
                        SpaceUserAddRequest spaceUserAddRequest = new SpaceUserAddRequest();
                        spaceUserAddRequest.setSpaceId(space.getId());
                        spaceUserAddRequest.setUserId(userId);
                        spaceUserAddRequest.setSpaceRole(SpaceRoleEnum.ADMIN.getValue());
                        spaceUserService.addSpaceUser(spaceUserAddRequest);
                    }
                    // 返回新写入的数据 id
                    return space.getId();
                });
                // 返回结果是包装类，可以做一些处理
                return Optional.ofNullable(newSpaceId).orElse(-1L);
            } finally {
                // 及时删除锁资源防止内存泄露
                locks.remove(userId);
            }
        }
    }

    @Override
    public Wrapper<Space> getQueryWrapper(SpaceQueryRequest spaceQueryRequest) {
        QueryWrapper<Space> queryWrapper = new QueryWrapper<>();
        Integer spaceLevel = spaceQueryRequest.getSpaceLevel();
        String spaceName = spaceQueryRequest.getSpaceName();
        Long userId = spaceQueryRequest.getUserId();
        Long id = spaceQueryRequest.getId();
        Integer spaceType = spaceQueryRequest.getSpaceType();
        queryWrapper.eq(ObjectUtil.isNotNull(id), "id", id);
        queryWrapper.eq(ObjectUtil.isNotNull(spaceLevel), "spaceLevel", spaceLevel);
        queryWrapper.eq(StrUtil.isNotBlank(spaceName), "spaceName", spaceName);
        queryWrapper.eq(ObjectUtil.isNotNull(userId), "userId", userId);
        queryWrapper.eq(ObjectUtil.isNotNull(spaceType), "spaceType", spaceType);
        return queryWrapper;
    }

    @Override
    public Page<SpaceVO> getSpaceVOPage(Page<Space> spacePage, HttpServletRequest request) {
        List<Space> spaceList = spacePage.getRecords();
        Page<SpaceVO> spaceVOPage = new Page<>(spacePage.getCurrent(), spacePage.getSize(), spacePage.getTotal());
        if (CollUtil.isEmpty(spaceList)) {
            return spaceVOPage;
        }
        // 对象列表 => 封装对象列表
        List<SpaceVO> spaceVOList = spaceList.stream().map(space -> {
            SpaceVO spaceVO = SpaceVO.objToVo(space);
            return spaceVO;
        }).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = spaceList.stream().map(Space::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream()
                .collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        spaceVOList.forEach(spaceVO -> {
            Long userId = spaceVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            spaceVO.setUser(userService.getUserVO(user));
        });
        spaceVOPage.setRecords(spaceVOList);
        return spaceVOPage;
    }

    @Override
    public SpaceVO getSpaceVO(Space space, HttpServletRequest request) {
        SpaceVO spaceVO = SpaceVO.objToVo(space);
        User user = userService.getById(space.getUserId());
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        spaceVO.setUser(userVO);
        return spaceVO;
    }

}




