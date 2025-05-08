package com.ywf.ywfpicturebackend.application.service.impl;


import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ywf.ywfpicturebackend.api.aliyunai.AliYunAiApi;
import com.ywf.ywfpicturebackend.api.aliyunai.CreateOutPaintingTaskRequest;
import com.ywf.ywfpicturebackend.api.aliyunai.CreateOutPaintingTaskResponse;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.infrastructure.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.interfaces.dto.picture.*;
import com.ywf.ywfpicturebackend.manager.TxYunCosManager;
import com.ywf.ywfpicturebackend.manager.upload.PictureUploadTemplate;
import com.ywf.ywfpicturebackend.manager.upload.TxYunFilePictureUpload;
import com.ywf.ywfpicturebackend.manager.upload.TxYunUrlPictureUpload;
import com.ywf.ywfpicturebackend.infrastructure.mapper.PictureMapper;
import com.ywf.ywfpicturebackend.interfaces.dto.file.UploadPictureResult;
import com.ywf.ywfpicturebackend.domain.user.entity.Picture;
import com.ywf.ywfpicturebackend.domain.user.entity.Space;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.domain.user.valueobj.PictureReviewStatusEnum;
import com.ywf.ywfpicturebackend.interfaces.vo.PictureVO;
import com.ywf.ywfpicturebackend.interfaces.vo.UserVO;
import com.ywf.ywfpicturebackend.application.service.PictureService;
import com.ywf.ywfpicturebackend.application.service.SpaceService;
import com.ywf.ywfpicturebackend.application.service.UserService;
import com.ywf.ywfpicturebackend.utils.ColorSimilarUtils;
import com.ywf.ywfpicturebackend.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author yiwenfeng
 * @description 针对表【picture(图片)】的数据库操作Service实现
 * @createDate 2025-04-26 15:46:08
 */
@Service
@Slf4j
public class PictureServiceImpl extends ServiceImpl<PictureMapper, Picture> implements PictureService {
    @Resource
    UserService userService;
    @Resource
    TxYunFilePictureUpload filePictureUpload;
    @Resource
    TxYunUrlPictureUpload urlPictureUpload;
    @Resource
    SpaceService spaceService;
    @Resource
    TxYunCosManager cosManager;
    @Resource
    private TransactionTemplate transactionTemplate;
    @Resource
    private PictureMapper pictureMapper;
    @Resource
    private AliYunAiApi aliYunAiApi;


    /**
     * 用户向某个空间传入图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    @Override
    public PictureVO uploadPicture(Object inputSource, PictureUploadRequest pictureUploadRequest, User loginUser) {
        if (inputSource == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片为空");
        }
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 空间权限校验
        Long spaceId = pictureUploadRequest.getSpaceId();
        if (spaceId != null) {
            Space space = spaceService.getById(spaceId);
            ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
//            // 权限校验使用SaToken+AOP切面完成必须空间创建人（管理员）才能上传
//            if (!loginUser.getId().equals(space.getUserId())) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间权限");
//            }
            // 校验额度
            if (space.getTotalCount() >= space.getMaxCount()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间条数不足");
            }
            if (space.getTotalSize() >= space.getMaxSize()) {
                throw new BusinessException(ErrorCode.OPERATION_ERROR, "空间大小不足");
            }
        }

        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 如果是更新图片，需要校验图片是否存在以及是否权限
        // 如果是更新图片，需要校验图片是否存在
        if (pictureId != null) {
            // 构造 QueryWrapper
            QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("id", pictureId)         // 根据主键 id 查询
                    .eq("spaceId", spaceId); // 附加 spaceId 条件
            // 执行查询
            Picture oldPicture = this.getOne(queryWrapper);
            ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR, "图片不存在");
//            // 仅本人或管理员可编辑
//            if (!oldPicture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
//                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
//            }
            // 校验空间是否一致
            // 没传 spaceId，则复用原有图片的 spaceId
            if (spaceId == null) {
                if (oldPicture.getSpaceId() != null) {
                    spaceId = oldPicture.getSpaceId();
                }
            } else {
                // 传了 spaceId，必须和原有图片一致
                if (ObjUtil.notEqual(spaceId, oldPicture.getSpaceId())) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "空间 id 不一致");
                }
            }
        }
        // 按照用户 id 划分目录 => 按照空间划分目录
        String uploadPathPrefix;
        if (spaceId == null) {
            // 无spaceId传入公共图库
            //uploadPathPrefix = String.format("public/%s", loginUser.getId());
            spaceId = 0L;// 公共图库0
        }
        uploadPathPrefix = String.format("space/%s", spaceId);
        // 根据 inputSource 类型区分上传方式
        PictureUploadTemplate pictureUploadTemplate = filePictureUpload;
        if (inputSource instanceof String) {
            pictureUploadTemplate = urlPictureUpload;
        }
        Picture picture = null;
        // 1. 校验图片
        pictureUploadTemplate.validPicture(inputSource);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = pictureUploadTemplate.getOriginFilename(inputSource);
        File file = null;
        String extName;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uuid, null);
            // 4.处理文件来源（本地或 URL）
            pictureUploadTemplate.processFile(inputSource, file);
            extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName) || !ImageUtils.validExtName(extName))
                extName = ImageUtils.getFileExtension(file);
            // md5判断是否上传过
            String md5 = SecureUtil.md5(file);
            List<Picture> samePictures = this.lambdaQuery().eq(Picture::getMd5, md5).eq(Picture::getSpaceId, spaceId).list();
            if (CollUtil.isNotEmpty(samePictures)) {
                picture = samePictures.get(0);
            }
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
        if (picture != null) {
            // 已经上传过
            log.info("{}已经上传过了...", picture);
            return PictureVO.objToVo(picture);
        }
        String picName = FileUtil.mainName(originFilename);
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, extName);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        UploadPictureResult uploadPictureResult = pictureUploadTemplate.uploadPicture(file, uploadPath, picName);
        picture = buildPicture(pictureUploadRequest, loginUser, uploadPictureResult, spaceId);
        // 开启事务
        Picture finalPicture = picture;
        transactionTemplate.execute(status -> {
            boolean result = this.save(finalPicture);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR, "图片上传失败");
            if (finalPicture.getSpaceId() != 0L) {
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, finalPicture.getSpaceId()).setSql("totalSize = totalSize + " + finalPicture.getPicSize()).setSql("totalCount = totalCount + 1").update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return finalPicture;
        });
        return PictureVO.objToVo(picture);
    }

    private Picture buildPicture(PictureUploadRequest pictureUploadRequest, User loginUser, UploadPictureResult uploadPictureResult, Long spaceId) {
        String picName = uploadPictureResult.getPicName();
        if (pictureUploadRequest != null && StrUtil.isNotBlank(pictureUploadRequest.getPicName())) {
            picName = pictureUploadRequest.getPicName();
        }
        // 用于判断是新增还是更新图片
        Long pictureId = null;
        if (pictureUploadRequest != null) {
            pictureId = pictureUploadRequest.getId();
        }
        // 构造要入库的图片信息
        Picture picture = new Picture();
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureUploadRequest.getTags()));
        picture.setUrl(uploadPictureResult.getUrl());
        picture.setName(picName);
        picture.setPicSize(uploadPictureResult.getPicSize());
        picture.setPicWidth(uploadPictureResult.getPicWidth());
        picture.setPicHeight(uploadPictureResult.getPicHeight());
        picture.setPicScale(uploadPictureResult.getPicScale());
        picture.setPicFormat(uploadPictureResult.getPicFormat());
        picture.setUserId(loginUser.getId());
        picture.setMd5(uploadPictureResult.getMd5());
        picture.setSpaceId(spaceId);
        picture.setPicColor(uploadPictureResult.getPicColor());


        // 补充审核参数
        fillReviewParams(picture, loginUser);
        return picture;
    }

    @Override
    public void fillReviewParams(Picture picture, User loginUser) {
        if (userService.isAdmin(loginUser)) {
            // 管理员自动过审
            picture.setReviewStatus(PictureReviewStatusEnum.PASS.getValue());
            picture.setReviewerId(loginUser.getId());
            picture.setReviewMessage("管理员自动过审");
            picture.setReviewTime(new Date());
        } else {
            // 非管理员，创建或编辑都要改为待审核
            picture.setReviewStatus(PictureReviewStatusEnum.REVIEWING.getValue());
        }
    }

    @Override
    public QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest) {
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        if (pictureQueryRequest == null) {
            return queryWrapper;
        }
        // 从对象中取值
        Long id = pictureQueryRequest.getId();
        String name = pictureQueryRequest.getName();
        String introduction = pictureQueryRequest.getIntroduction();
        String category = pictureQueryRequest.getCategory();
        List<String> tags = pictureQueryRequest.getTags();
        Long picSize = pictureQueryRequest.getPicSize();
        Integer picWidth = pictureQueryRequest.getPicWidth();
        Integer picHeight = pictureQueryRequest.getPicHeight();
        Double picScale = pictureQueryRequest.getPicScale();
        String picFormat = pictureQueryRequest.getPicFormat();
        String searchText = pictureQueryRequest.getSearchText();
        Long userId = pictureQueryRequest.getUserId();
        String sortField = pictureQueryRequest.getSortField();
        String sortOrder = pictureQueryRequest.getSortOrder();
        Long reviewerId = pictureQueryRequest.getReviewerId();
        Integer reviewStatus = pictureQueryRequest.getReviewStatus();
        String reviewMessage = pictureQueryRequest.getReviewMessage();
        Long spaceId = pictureQueryRequest.getSpaceId();
        Boolean nullSpaceId = pictureQueryRequest.getNullSpaceId();
        nullSpaceId = ObjUtil.isNotEmpty(nullSpaceId) ? nullSpaceId : false;
        Date startEditTime = pictureQueryRequest.getStartEditTime();
        Date endEditTime = pictureQueryRequest.getEndEditTime();
        queryWrapper.ge(ObjUtil.isNotEmpty(startEditTime), "editTime", startEditTime);
        queryWrapper.lt(ObjUtil.isNotEmpty(endEditTime), "editTime", endEditTime);
        // 从多字段中搜索
        if (StrUtil.isNotBlank(searchText)) {
            // 需要拼接查询条件
            queryWrapper.and(qw -> qw.like("name", searchText).or().like("introduction", searchText));
        }
        queryWrapper.eq(ObjUtil.isNotEmpty(id), "id", id);
        queryWrapper.eq(ObjUtil.isNotEmpty(userId), "userId", userId);
        queryWrapper.like(StrUtil.isNotBlank(name), "name", name);
        queryWrapper.like(StrUtil.isNotBlank(introduction), "introduction", introduction);
        queryWrapper.like(StrUtil.isNotBlank(picFormat), "picFormat", picFormat);
        queryWrapper.eq(StrUtil.isNotBlank(category), "category", category);
        queryWrapper.eq(ObjUtil.isNotEmpty(picWidth), "picWidth", picWidth);
        queryWrapper.eq(ObjUtil.isNotEmpty(picHeight), "picHeight", picHeight);
        queryWrapper.eq(ObjUtil.isNotEmpty(picSize), "picSize", picSize);
        queryWrapper.eq(ObjUtil.isNotEmpty(picScale), "picScale", picScale);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewerId), "reviewerId", reviewerId);
        queryWrapper.eq(ObjUtil.isNotEmpty(reviewStatus), "reviewStatus", reviewStatus);
        queryWrapper.like(StrUtil.isNotBlank(reviewMessage), "reviewMessage", reviewMessage);
        queryWrapper.eq(ObjUtil.isNotEmpty(spaceId), "spaceId", spaceId);// 查询指定图库空间
        queryWrapper.eq(nullSpaceId, "spaceId", 0L);// 是否只查spaceId为空（即公共图库）
        // JSON 数组查询
        if (CollUtil.isNotEmpty(tags)) {
            for (String tag : tags) {
                queryWrapper.like("tags", "\"" + tag + "\"");
            }
        }
        // 排序
        queryWrapper.orderBy(StrUtil.isNotEmpty(sortField), sortOrder.equals("ascend"), sortField);
        return queryWrapper;
    }

    @Override
    public PictureVO getPictureVO(Picture picture, HttpServletRequest request) {
        // 对象转封装类
        PictureVO pictureVO = PictureVO.objToVo(picture);
        // 关联查询用户信息
        Long userId = picture.getUserId();
        if (userId != null && userId > 0) {
            User user = userService.getById(userId);
            UserVO userVO = userService.getUserVO(user);
            pictureVO.setUser(userVO);
        }
        return pictureVO;
    }

    /**
     * 分页获取图片封装
     */
    @Override
    public Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request) {
        List<Picture> pictureList = picturePage.getRecords();
        Page<PictureVO> pictureVOPage = new Page<>(picturePage.getCurrent(), picturePage.getSize(), picturePage.getTotal());
        if (CollUtil.isEmpty(pictureList)) {
            return pictureVOPage;
        }
        // 对象列表 => 封装对象列表
        List<PictureVO> pictureVOList = pictureList.stream().map(PictureVO::objToVo).collect(Collectors.toList());
        // 1. 关联查询用户信息
        Set<Long> userIdSet = pictureList.stream().map(Picture::getUserId).collect(Collectors.toSet());
        Map<Long, List<User>> userIdUserListMap = userService.listByIds(userIdSet).stream().collect(Collectors.groupingBy(User::getId));
        // 2. 填充信息
        pictureVOList.forEach(pictureVO -> {
            Long userId = pictureVO.getUserId();
            User user = null;
            if (userIdUserListMap.containsKey(userId)) {
                user = userIdUserListMap.get(userId).get(0);
            }
            pictureVO.setUser(userService.getUserVO(user));
        });
        pictureVOPage.setRecords(pictureVOList);
        return pictureVOPage;
    }

    @Override
    public void validPicture(Picture picture) {
        ThrowUtils.throwIf(picture == null, ErrorCode.PARAMS_ERROR);
        // 从对象中取值
        Long id = picture.getId();
        String url = picture.getUrl();
        String introduction = picture.getIntroduction();
        // 修改数据时，id 不能为空，有参数则校验
        ThrowUtils.throwIf(ObjUtil.isNull(id), ErrorCode.PARAMS_ERROR, "id 不能为空");
        if (StrUtil.isNotBlank(url)) {
            ThrowUtils.throwIf(url.length() > 1024, ErrorCode.PARAMS_ERROR, "url 过长");
        }
        if (StrUtil.isNotBlank(introduction)) {
            ThrowUtils.throwIf(introduction.length() > 800, ErrorCode.PARAMS_ERROR, "简介过长");
        }
    }

    @Override
    public void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser) {
        Long id = pictureReviewRequest.getId();
        Integer reviewStatus = pictureReviewRequest.getReviewStatus();
        PictureReviewStatusEnum reviewStatusEnum = PictureReviewStatusEnum.getEnumByValue(reviewStatus);
        if (id == null || reviewStatusEnum == null || PictureReviewStatusEnum.REVIEWING.equals(reviewStatusEnum)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 判断是否存在
        Picture oldPicture = this.lambdaQuery().eq(Picture::getSpaceId, pictureReviewRequest.getSpaceId()).eq(Picture::getId, id).one();
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 是否已审核
        if (oldPicture.getReviewStatus().equals(reviewStatus)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请勿重复审核");
        }
        // 更新审核状态
        Picture updatePicture = new Picture();
        BeanUtils.copyProperties(pictureReviewRequest, updatePicture);
        updatePicture.setReviewerId(loginUser.getId());
        updatePicture.setReviewTime(new Date());
        int result = pictureMapper.updatePictureReviewStatus(updatePicture);
        ThrowUtils.throwIf(result != 1, ErrorCode.OPERATION_ERROR);
    }

    @Override
    public Integer uploadPictureByBatch(PictureUploadByBatchRequest pictureUploadByBatchRequest, User loginUser) {
        String searchText = pictureUploadByBatchRequest.getSearchText();
        // 格式化数量
        Integer count = pictureUploadByBatchRequest.getCount();
        ThrowUtils.throwIf(count > 30, ErrorCode.PARAMS_ERROR, "最多 30 条");
        // 要抓取的地址
        String fetchUrl = String.format("https://cn.bing.com/images/async?q=%s&mmasync=1", searchText);
        Document document;
        try {
            document = Jsoup.connect(fetchUrl).get();
        } catch (IOException e) {
            log.error("获取页面失败", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取页面失败");
        }
        Element div = document.getElementsByClass("dgControl").first();
        if (ObjUtil.isNull(div)) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "获取元素失败");
        }
        Elements imgElementList = div.select("img.mimg");
        int uploadCount = 0;
        String namePrefix = pictureUploadByBatchRequest.getNamePrefix();
        if (StrUtil.isBlank(namePrefix)) {
            namePrefix = searchText;
        }
        List<String> tagList = pictureUploadByBatchRequest.getTags();
        // 上传图片
        PictureUploadRequest pictureUploadRequest = new PictureUploadRequest();
        for (Element imgElement : imgElementList) {
            String fileUrl = imgElement.attr("src");
            if (StrUtil.isBlank(fileUrl)) {
                log.info("当前链接为空，已跳过: {}", fileUrl);
                continue;
            }
            // 处理图片上传地址，防止出现转义问题
            int questionMarkIndex = fileUrl.indexOf("?");
            if (questionMarkIndex > -1) {
                fileUrl = fileUrl.substring(0, questionMarkIndex);
            }

            if (StrUtil.isNotBlank(namePrefix)) {
                // 设置图片名称，序号连续递增
                pictureUploadRequest.setPicName(namePrefix + (uploadCount + 1));
            }
            pictureUploadRequest.setTags(tagList);
            try {
                PictureVO pictureVO = this.uploadPicture(fileUrl, pictureUploadRequest, loginUser);
                log.info("图片上传成功, id = {}", pictureVO.getId());
                uploadCount++;
            } catch (Exception e) {
                log.error("图片上传失败", e);
                continue;
            }
            if (uploadCount >= count) {
                break;
            }
        }
        return uploadCount;
    }

    @Deprecated
    @Override
    public void checkPictureAuth(User loginUser, Picture picture) {
        Long spaceId = picture.getSpaceId();
        if (spaceId == null) {
            // 公共图库，仅本人或管理员可操作
            if (!picture.getUserId().equals(loginUser.getId()) && !userService.isAdmin(loginUser)) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        } else {
            // 私有空间，仅空间管理员可操作
            if (!picture.getUserId().equals(loginUser.getId())) {
                throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
            }
        }
    }

    @Async
    @Override
    public void clearPictureFile(Picture oldPicture) {
        // 判断该图片是否被多条记录使用
        String pictureUrl = oldPicture.getUrl();
        long count = this.lambdaQuery().eq(Picture::getUrl, pictureUrl).eq(Picture::getSpaceId, oldPicture.getSpaceId()).count();
        // 有不止一条记录用到了该图片，不清理
        if (count > 1) {
            return;
        }
        try {
            cosManager.deleteObject(oldPicture.getUrl());
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean deletePicture(PictureDeleteRequest pictureDeleteRequest, User loginUser) {
        ThrowUtils.throwIf(pictureDeleteRequest == null || pictureDeleteRequest.getId() <= 0, ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NOT_LOGIN_ERROR);
        Long spaceId = pictureDeleteRequest.getSpaceId();
        if (spaceId == null) spaceId = 0L;
        QueryWrapper<Picture> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("id", pictureDeleteRequest.getId());
        queryWrapper.eq("spaceId", spaceId);
        // 判断是否存在
        Picture oldPicture = this.getOne(queryWrapper);
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 开启事务
        transactionTemplate.execute(status -> {
            // 操作数据库
            boolean result = this.remove(queryWrapper);
            ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
            // 释放额度
            if (oldPicture.getSpaceId() != 0) {
                boolean update = spaceService.lambdaUpdate().eq(Space::getId, oldPicture.getSpaceId()).setSql("totalSize = totalSize - " + oldPicture.getPicSize()).setSql("totalCount = totalCount - 1").update();
                ThrowUtils.throwIf(!update, ErrorCode.OPERATION_ERROR, "额度更新失败");
            }
            return true;
        });
        // 异步清理文件
        this.clearPictureFile(oldPicture);
        return true;
    }

    @Override
    public boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser) {
        // 在此处将实体类和 DTO 进行转换
        Picture picture = new Picture();
        BeanUtils.copyProperties(pictureEditRequest, picture);
        // 注意将 list 转为 string
        picture.setTags(JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        // 设置编辑时间
        picture.setEditTime(new Date());
        // 数据校验
        this.validPicture(picture);
        // 判断是否存在
        Picture oldPicture = this.lambdaQuery().eq(Picture::getId, pictureEditRequest.getId()).eq(Picture::getSpaceId, picture.getSpaceId()).one();
        ThrowUtils.throwIf(oldPicture == null, ErrorCode.NOT_FOUND_ERROR);
        // 补充审核参数
        this.fillReviewParams(picture, loginUser);
        // 操作数据库
        LambdaUpdateWrapper<Picture> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(Picture::getId, pictureEditRequest.getId());
        updateWrapper.eq(Picture::getSpaceId, pictureEditRequest.getSpaceId());
        updateWrapper.set(Picture::getTags, JSONUtil.toJsonStr(pictureEditRequest.getTags()));
        updateWrapper.set(Picture::getEditTime, new Date());
        updateWrapper.set(Picture::getName, pictureEditRequest.getName());
        updateWrapper.set(Picture::getIntroduction, pictureEditRequest.getIntroduction());
        updateWrapper.set(Picture::getCategory, pictureEditRequest.getCategory());
        boolean result = this.update(updateWrapper);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser) {
        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || StrUtil.isBlank(picColor), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        // 3. 查询该空间下所有图片（必须有主色调）
        List<Picture> pictureList = this.lambdaQuery().eq(Picture::getSpaceId, spaceId).isNotNull(Picture::getPicColor).list();
        // 如果没有图片，直接返回空列表
        if (CollUtil.isEmpty(pictureList)) {
            return Collections.emptyList();
        }
        // 将目标颜色转为 Color 对象
        Color targetColor = Color.decode(picColor);
        // 4. 计算相似度并排序
        List<Picture> sortedPictures = pictureList.stream().sorted(Comparator.comparingDouble(picture -> {
                    // 提取图片主色调
                    String hexColor = picture.getPicColor();
                    // 没有主色调的图片放到最后
                    if (StrUtil.isBlank(hexColor)) {
                        return Double.MAX_VALUE;
                    }
                    Color pictureColor = Color.decode(hexColor);
                    // 越大越相似
                    return -ColorSimilarUtils.calculateSimilarity(targetColor, pictureColor);
                }))
                // 取前 12 个
                .limit(12).collect(Collectors.toList());

        // 转换为 PictureVO
        return sortedPictures.stream().map(PictureVO::objToVo).collect(Collectors.toList());
    }

    /**
     * nameRule 格式：图片{序号}
     *
     * @param pictureList
     * @param nameRule
     */
    private void fillPictureWithNameRule(List<Picture> pictureList, String nameRule) {
        if (CollUtil.isEmpty(pictureList) || StrUtil.isBlank(nameRule)) {
            return;
        }
        long count = 1;
        try {
            for (Picture picture : pictureList) {
                String pictureName = nameRule.replaceAll("\\{序号}", String.valueOf(count++));
                picture.setName(pictureName);
            }
        } catch (Exception e) {
            log.error("名称解析错误", e);
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "名称解析错误");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser) {
        List<Long> pictureIdList = pictureEditByBatchRequest.getPictureIdList();
        Long spaceId = pictureEditByBatchRequest.getSpaceId();
        String category = pictureEditByBatchRequest.getCategory();
        List<String> tags = pictureEditByBatchRequest.getTags();
        String nameRule = pictureEditByBatchRequest.getNameRule();

        // 1. 校验参数
        ThrowUtils.throwIf(spaceId == null || CollUtil.isEmpty(pictureIdList), ErrorCode.PARAMS_ERROR);
        ThrowUtils.throwIf(loginUser == null, ErrorCode.NO_AUTH_ERROR);
        // 2. 校验空间权限
        Space space = spaceService.getById(spaceId);
        ThrowUtils.throwIf(space == null, ErrorCode.NOT_FOUND_ERROR, "空间不存在");
        if (!loginUser.getId().equals(space.getUserId())) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "没有空间访问权限");
        }

        // 3. 查询指定图片，仅选择需要的字段
        List<Picture> pictureList = this.lambdaQuery().select(Picture::getId, Picture::getSpaceId).eq(Picture::getSpaceId, spaceId).in(Picture::getId, pictureIdList).list();
        fillPictureWithNameRule(pictureList, nameRule);
        if (pictureList.isEmpty()) {
            return;
        }
        // 4. 更新分类和标签
        pictureList.forEach(picture -> {
            if (StrUtil.isNotBlank(category)) {
                picture.setCategory(category);
            }
            if (CollUtil.isNotEmpty(tags)) {
                picture.setTags(JSONUtil.toJsonStr(tags));
            }
        });
        int updated = 0;
        for (Picture picture : pictureList) {
            updated += pictureMapper.updateByShardingKey(picture);
        }
        // 5. 批量更新
        ThrowUtils.throwIf(updated != pictureList.size(), ErrorCode.OPERATION_ERROR);
    }

    @Override
    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser) {
        // 获取图片信息
        Long pictureId = createPictureOutPaintingTaskRequest.getPictureId();
        Long spaceId = createPictureOutPaintingTaskRequest.getSpaceId();
        Picture picture = Optional.ofNullable(this.lambdaQuery().eq(Picture::getSpaceId, spaceId).eq(Picture::getId, pictureId).one()).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ERROR));
        // 权限校验
        checkPictureAuth(loginUser, picture);
        // 构造请求参数
        CreateOutPaintingTaskRequest taskRequest = new CreateOutPaintingTaskRequest();
        CreateOutPaintingTaskRequest.Input input = new CreateOutPaintingTaskRequest.Input();
        input.setImageUrl(picture.getUrl());
        taskRequest.setInput(input);
        BeanUtil.copyProperties(createPictureOutPaintingTaskRequest, taskRequest);
        // 创建任务
        return aliYunAiApi.createOutPaintingTask(taskRequest);
    }

}




