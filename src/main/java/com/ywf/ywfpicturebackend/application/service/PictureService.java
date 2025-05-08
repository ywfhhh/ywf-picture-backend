package com.ywf.ywfpicturebackend.application.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.ywf.ywfpicturebackend.api.aliyunai.CreateOutPaintingTaskResponse;
import com.ywf.ywfpicturebackend.interfaces.dto.picture.*;
import com.ywf.ywfpicturebackend.domain.user.entity.Picture;
import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.interfaces.vo.PictureVO;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * @author yiwenfeng
 * @description 针对表【picture(图片)】的数据库操作Service
 * @createDate 2025-04-26 15:46:08
 */
public interface PictureService extends IService<Picture> {
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

    void validPicture(Picture picture);

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

}
