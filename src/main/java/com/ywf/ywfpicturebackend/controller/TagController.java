package com.ywf.ywfpicturebackend.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ywf.ywfpicturebackend.infrastructure.annotation.AuthCheck;
import com.ywf.ywfpicturebackend.infrastructure.common.BaseResponse;
import com.ywf.ywfpicturebackend.infrastructure.common.DeleteRequest;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.infrastructure.common.ResultUtils;
import com.ywf.ywfpicturebackend.domain.user.constant.UserConstant;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.infrastructure.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.interfaces.dto.tag.TagAddRequest;
import com.ywf.ywfpicturebackend.interfaces.dto.tag.TagQueryRequest;
import com.ywf.ywfpicturebackend.domain.user.entity.Tag;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.interfaces.vo.TagVO;
import com.ywf.ywfpicturebackend.application.service.TagService;
import com.ywf.ywfpicturebackend.application.service.UserService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.Date;

@RestController
@RequestMapping("tag")
public class TagController {
    @Resource
    private TagService tagService;
    @Resource
    private UserService userService;

    /**
     * 分页获取标签列表（仅管理员可用）
     */
    @PostMapping("/list/page")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<TagVO>> listTagByPage(@RequestBody TagQueryRequest tagQueryRequest) {
        long current = tagQueryRequest.getCurrent();
        long size = tagQueryRequest.getPageSize();
        // 查询数据库
        Page<Tag> tagPage = tagService.page(new Page<>(current, size),
                tagService.getQueryWrapper(tagQueryRequest));
        return ResultUtils.success(tagService.getTagVOPage(tagPage));
    }

    /**
     * 编辑Tag
     */
    @PostMapping("/saveOrUpdate")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> addTag(@RequestBody TagAddRequest tagAddRequest, HttpServletRequest request) {
        if (tagAddRequest == null || tagAddRequest.getTagName().equals("")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        // 在此处将实体类和 DTO 进行转换
        Tag tag = new Tag();
        BeanUtils.copyProperties(tagAddRequest, tag);
        // 设置编辑时间
        tag.setEditTime(new Date());
        // 设置创建者id
        tag.setUserId(loginUser.getId());
        // 判断是否存在
        TagQueryRequest tagQueryRequest = new TagQueryRequest();
        tagQueryRequest.setTagName(tagAddRequest.getTagName());
        Tag oldTag = tagService.getOne(tagService.getQueryWrapper(tagQueryRequest));
        ThrowUtils.throwIf(oldTag != null, ErrorCode.PARAMS_ERROR, "重复的Tag!");
        // 操作数据库
        boolean result = tagService.saveOrUpdate(tag);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }

    /**
     * 删除Tag
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteTag(@RequestBody DeleteRequest deleteRequest, HttpServletRequest request) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long id = deleteRequest.getId();
        // 判断是否存在
        Tag oldTag = tagService.getById(id);
        ThrowUtils.throwIf(oldTag == null, ErrorCode.NOT_FOUND_ERROR);
        // 操作数据库
        boolean result = tagService.removeById(id);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(true);
    }
}
