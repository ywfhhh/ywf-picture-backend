package com.ywf.ywfpicturebackend.domain.picture.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ywf.ywfpicturebackend.infrastructure.api.aliyunai.CreateOutPaintingTaskResponse;
import com.ywf.ywfpicturebackend.domain.picture.entity.Picture;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.interfaces.dto.picture.*;
import com.ywf.ywfpicturebackend.interfaces.vo.picture.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author yiwenfeng
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-04-26 15:46:08
 */
public interface PictureDomainService {
    /**
     * 上传图片
     *
     * @param inputSource
     * @param pictureUploadRequest
     * @param loginUser
     * @return
     */
    PictureVO uploadPicture(Object inputSource,
                            PictureUploadRequest pictureUploadRequest,
                            User loginUser);

    QueryWrapper<Picture> getQueryWrapper(PictureQueryRequest pictureQueryRequest);

    PictureVO getPictureVO(Picture picture, HttpServletRequest request);

    Page<PictureVO> getPictureVOPage(Page<Picture> picturePage, HttpServletRequest request);


    /**
     * 图片审核
     *
     * @param pictureReviewRequest
     * @param loginUser
     */
    void doPictureReview(PictureReviewRequest pictureReviewRequest, User loginUser);

    void fillReviewParams(Picture picture, User loginUser);

    /**
     * 批量抓取和创建图片
     *
     * @param pictureUploadByBatchRequest
     * @param loginUser
     * @return 成功创建的图片数
     */
    Integer uploadPictureByBatch(
            PictureUploadByBatchRequest pictureUploadByBatchRequest,
            User loginUser
    );

    void checkPictureAuth(User loginUser, Picture picture);

    void clearPictureFile(Picture oldPicture);

    boolean deletePicture(PictureDeleteRequest pictureDeleteRequest, User loginUser);

    boolean editPicture(PictureEditRequest pictureEditRequest, User loginUser);

    public List<PictureVO> searchPictureByColor(Long spaceId, String picColor, User loginUser);

    public void editPictureByBatch(PictureEditByBatchRequest pictureEditByBatchRequest, User loginUser);

    public CreateOutPaintingTaskResponse createPictureOutPaintingTask(CreatePictureOutPaintingTaskRequest createPictureOutPaintingTaskRequest, User loginUser);

    Picture getById(Long id, Long spaceId);

    boolean updateById(Picture picture, HttpServletRequest request);

    Page<Picture> page(Page<Picture> picturePage, QueryWrapper<Picture> queryWrapper);

    List<Object> selectObjs(QueryWrapper<Picture> queryWrapper);
}
