package com.ywf.ywfpicturebackend.interfaces.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ywf.ywfpicturebackend.application.service.PictureApplicationService;
import com.ywf.ywfpicturebackend.application.service.UserApplicationService;
import com.ywf.ywfpicturebackend.infrastructure.annotation.AuthCheck;
import com.ywf.ywfpicturebackend.infrastructure.common.BaseResponse;
import com.ywf.ywfpicturebackend.infrastructure.common.DeleteRequest;
import com.ywf.ywfpicturebackend.infrastructure.common.ErrorCode;
import com.ywf.ywfpicturebackend.infrastructure.common.ResultUtils;
import com.ywf.ywfpicturebackend.domain.user.constant.UserConstant;
import com.ywf.ywfpicturebackend.infrastructure.exception.BusinessException;
import com.ywf.ywfpicturebackend.infrastructure.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.interfaces.assembler.UserAssembler;
import com.ywf.ywfpicturebackend.interfaces.dto.user.*;
import com.ywf.ywfpicturebackend.infrastructure.manager.upload.TxYunFilePictureUpload;
import com.ywf.ywfpicturebackend.interfaces.dto.file.UploadPictureResult;
import com.ywf.ywfpicturebackend.domain.picture.entity.Picture;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.interfaces.vo.user.LoginUserVO;
import com.ywf.ywfpicturebackend.interfaces.vo.user.UserVO;
import com.ywf.ywfpicturebackend.infrastructure.utils.ImageUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.util.Date;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @Resource
    private UserApplicationService userService;
    @Resource
    TxYunFilePictureUpload filePictureUpload;
    @Resource
    PictureApplicationService pictureService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        long result = userService.userRegister(userRegisterRequest);
        return ResultUtils.success(result);
    }

    /**
     * 用户修改
     */
    @PostMapping("/updateAvatar")
    public BaseResponse<String> userUpdateAvatar(
            @RequestPart("file") MultipartFile multipartFile,
            HttpServletRequest request) {
        ThrowUtils.throwIf(multipartFile == null, ErrorCode.PARAMS_ERROR);
        User loginUser = userService.getLoginUser(request);
        String url = getUrl(multipartFile, loginUser);
        User user = new User();
        user.setId(loginUser.getId());
        user.setUserAvatar(url);
        boolean result = userService.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(url);
    }

    private String getUrl(MultipartFile multipartFile, User loginUser) {
        String uploadPathPrefix = String.format("userAvatar/%s", loginUser.getId());
        Picture picture = null;
        // 1. 校验图片
        filePictureUpload.validPicture(multipartFile);
        // 2. 图片上传地址
        String uuid = RandomUtil.randomString(16);
        String originFilename = filePictureUpload.getOriginFilename(multipartFile);
        File file = null;
        String extName;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uuid, null);
            // 4.处理文件来源（本地或 URL）
            filePictureUpload.processFile(multipartFile, file);
            extName = FileUtil.extName(file);
            if (StrUtil.isBlank(extName) || !ImageUtils.validExtName(extName))
                extName = ImageUtils.getFileExtension(file);
            // md5判断是否上传过
            String md5 = SecureUtil.md5(file);
            List<Picture> samePictures = pictureService.lambdaQuery().eq(Picture::getMd5, md5).eq(Picture::getUserId, loginUser.getId()).eq(Picture::getSpaceId, 0L).list();
            if (CollUtil.isNotEmpty(samePictures)) {
                picture = samePictures.get(0);
            }
        } catch (Exception e) {
            log.error("图片上传到对象存储失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败");
        }
        if (picture != null) {
            return picture.getUrl();
        }
        String picName = FileUtil.mainName(originFilename);
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid, extName);
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(file, uploadPath, picName);
        return uploadPictureResult.getUrl();
    }

    /**
     * 用户修改
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> userUpdate(
            @RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.updateById(UserAssembler.toUserEntity(userUpdateRequest));
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(result);
    }

    @GetMapping("/get/login")
    public BaseResponse<LoginUserVO> getLoginUser(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        return ResultUtils.success(userService.getLoginUserVO(loginUser));
    }

    @PostMapping("/login")
    public BaseResponse<LoginUserVO> userLogin(@RequestBody UserLoginRequest userLoginRequest, HttpServletRequest request) {
        ThrowUtils.throwIf(userLoginRequest == null, ErrorCode.PARAMS_ERROR);
        LoginUserVO loginUserVO = userService.userLogin(userLoginRequest, request);
        return ResultUtils.success(loginUserVO);
    }


    @PostMapping("/logout")
    public BaseResponse<Boolean> userLogout(HttpServletRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = userService.userLogout(request);
        return ResultUtils.success(result);
    }


    /**
     * 创建用户
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addUser(@RequestBody UserAddRequest userAddRequest) {
        ThrowUtils.throwIf(userAddRequest == null, ErrorCode.PARAMS_ERROR);
        long userId = userService.addUser(UserAssembler.toUserEntity(userAddRequest));
        return ResultUtils.success(userId);
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        return ResultUtils.success(userService.getById(id));
    }

    /**
     * 根据 id 获取包装类
     */
    @GetMapping("/get/vo")
    public BaseResponse<UserVO> getUserVOById(long id) {
        BaseResponse<User> response = getUserById(id);
        User user = response.getData();
        return ResultUtils.success(userService.getUserVO(user));
    }

    /**
     * 删除用户
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deleteUser(@RequestBody DeleteRequest deleteRequest) {
        if (deleteRequest == null || deleteRequest.getId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        boolean b = userService.removeById(deleteRequest);
        return ResultUtils.success(b);
    }

    /**
     * 分页获取用户封装列表（仅管理员）
     *
     * @param userQueryRequest 查询请求参数
     */
    @PostMapping("/list/page/vo")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Page<UserVO>> listUserVOByPage(@RequestBody UserQueryRequest userQueryRequest) {
        ThrowUtils.throwIf(userQueryRequest == null, ErrorCode.PARAMS_ERROR);
        Page<UserVO> userVOPage = userService.listUserVOByPage(userQueryRequest);
        return ResultUtils.success(userVOPage);
    }

}
