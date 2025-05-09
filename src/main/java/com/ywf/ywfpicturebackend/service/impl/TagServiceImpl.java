package com.ywf.ywfpicturebackend.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.mapper.TagMapper;
import com.ywf.ywfpicturebackend.model.dto.tag.TagQueryRequest;
import com.ywf.ywfpicturebackend.model.entity.Tag;
import com.ywf.ywfpicturebackend.model.entity.User;
import com.ywf.ywfpicturebackend.model.vo.PictureVO;
import com.ywf.ywfpicturebackend.model.vo.TagVO;
import com.ywf.ywfpicturebackend.service.TagService;
import com.ywf.ywfpicturebackend.service.UserService;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author yiwenfeng
 * @description 针对表【tag(标签表)】的数据库操作Service实现
 * @createDate 2025-04-27 01:50:56
 */
@Service
public class TagServiceImpl extends ServiceImpl<TagMapper, Tag>
        implements TagService {
    @Resource
    private UserService userService;

    @Override
    public QueryWrapper<Tag> getQueryWrapper(TagQueryRequest tagQueryRequest) {
        QueryWrapper<Tag> tagQueryWrapper = new QueryWrapper<>();
        Long id = tagQueryRequest.getId();
        Long userId = tagQueryRequest.getUserId();
        String tagName = tagQueryRequest.getTagName();
        tagQueryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        tagQueryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        tagQueryWrapper.like(StrUtil.isNotBlank(tagName), "tagName", tagName);
        return tagQueryWrapper;
    }

    @Override
    public Page<TagVO> getTagVOPage(Page<Tag> tagPage) {
        Page<TagVO> tagVOPage = new Page<>(tagPage.getCurrent(), tagPage.getSize(), tagPage.getTotal());
        List<Tag> tagList = tagPage.getRecords();
        if (CollUtil.isEmpty(tagList)) {
            return tagVOPage;
        }
        Map<Long, String> userIdToName = new HashMap<>();
        List<User> userList = userService.list();
        for (User user : userList) {
            userIdToName.put(user.getId(), user.getUserName());
        }
        List<TagVO> tagVOList = tagList.stream().map(tag -> {
            TagVO tagVo = Tag.objToVO(tag);
            tagVo.setUserName(userIdToName.get(tag.getUserId()));
            return tagVo;
        }).collect(Collectors.toList());
        tagVOPage.setRecords(tagVOList);
        return tagVOPage;
    }
}




