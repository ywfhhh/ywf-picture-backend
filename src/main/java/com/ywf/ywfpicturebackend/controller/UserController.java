package com.ywf.ywfpicturebackend.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.SecureUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ywf.ywfpicturebackend.annotation.AuthCheck;
import com.ywf.ywfpicturebackend.common.BaseResponse;
import com.ywf.ywfpicturebackend.common.DeleteRequest;
import com.ywf.ywfpicturebackend.common.ErrorCode;
import com.ywf.ywfpicturebackend.common.ResultUtils;
import com.ywf.ywfpicturebackend.constant.UserConstant;
import com.ywf.ywfpicturebackend.exception.BusinessException;
import com.ywf.ywfpicturebackend.exception.ThrowUtils;
import com.ywf.ywfpicturebackend.manager.upload.FilePictureUpload;
import com.ywf.ywfpicturebackend.manager.upload.PictureUploadTemplate;
import com.ywf.ywfpicturebackend.model.dto.file.UploadPictureResult;
import com.ywf.ywfpicturebackend.model.dto.user.*;
import com.ywf.ywfpicturebackend.model.entity.Picture;
import com.ywf.ywfpicturebackend.model.entity.User;
import com.ywf.ywfpicturebackend.model.vo.LoginUserVO;
import com.ywf.ywfpicturebackend.model.vo.PictureVO;
import com.ywf.ywfpicturebackend.model.vo.UserVO;
import com.ywf.ywfpicturebackend.service.PictureService;
import com.ywf.ywfpicturebackend.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
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
    private UserService userService;
    @Resource
    FilePictureUpload filePictureUpload;
    @Resource
    PictureService pictureService;

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public BaseResponse<Long> userRegister(@RequestBody UserRegisterRequest userRegisterRequest) {
        ThrowUtils.throwIf(userRegisterRequest == null, ErrorCode.PARAMS_ERROR);
        String userAccount = userRegisterRequest.getUserAccount();
        String userPassword = userRegisterRequest.getUserPassword();
        String checkPassword = userRegisterRequest.getCheckPassword();
        long result = userService.userRegister(userAccount, userPassword, checkPassword);
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
        String uploadFilename = String.format("%s_%s.%s", DateUtil.formatDate(new Date()), uuid,
                FileUtil.getSuffix(originFilename));
        String uploadPath = String.format("%s/%s", uploadPathPrefix, uploadFilename);
        File file = null;
        try {
            // 3. 创建临时文件
            file = File.createTempFile(uploadPath, null);
            // 4.处理文件来源（本地或 URL）
            filePictureUpload.processFile(multipartFile, file);
            // md5判断是否上传过
            String md5 = SecureUtil.md5(file);
            List<Picture> samePictures = pictureService.lambdaQuery().eq(Picture::getMd5, md5).eq(Picture::getUserId, loginUser.getId()).list();
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
        UploadPictureResult uploadPictureResult = filePictureUpload.uploadPicture(file, uploadPathPrefix, picName);
        return uploadPictureResult.getUrl();
    }

    /**
     * 用户修改
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> userUpdate(
            @RequestBody UserUpdateRequest userUpdateRequest) {
        ThrowUtils.throwIf(userUpdateRequest == null, ErrorCode.PARAMS_ERROR);
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = userService.updateById(user);
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
        String userAccount = userLoginRequest.getUserAccount();
        String userPassword = userLoginRequest.getUserPassword();
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>().eq("userAccount", userAccount);
        User loginUser = userService.getOne(queryWrapper);
        if (loginUser == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户不存在!");
        }
        if (!loginUser.getUserPassword().equals(userService.getEncryptPassword(userPassword))) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码错误!");
        }
        request.getSession().setAttribute(UserConstant.USER_LOGIN_STATE, loginUser);
        LoginUserVO loginUserVO = userService.getLoginUserVO(loginUser);
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
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);
        // 默认密码 123123
        final String DEFAULT_PASSWORD = "123123";
        String encryptPassword = userService.getEncryptPassword(DEFAULT_PASSWORD);
        user.setUserPassword(encryptPassword);
        boolean result = userService.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return ResultUtils.success(user.getId());
    }

    /**
     * 根据 id 获取用户（仅管理员）
     */
    @GetMapping("/get")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<User> getUserById(long id) {
        ThrowUtils.throwIf(id <= 0, ErrorCode.PARAMS_ERROR);
        User user = userService.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return ResultUtils.success(user);
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
        boolean b = userService.removeById(deleteRequest.getId());
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
        long current = userQueryRequest.getCurrent();
        long pageSize = userQueryRequest.getPageSize();
        Page<User> userPage = userService.page(new Page<>(current, pageSize),
                userService.getQueryWrapper(userQueryRequest));
        Page<UserVO> userVOPage = new Page<>(current, pageSize, userPage.getTotal());
        List<UserVO> userVOList = userService.getUserVOList(userPage.getRecords());
        userVOPage.setRecords(userVOList);
        return ResultUtils.success(userVOPage);
    }

}
