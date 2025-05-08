package com.ywf.ywfpicturebackend.infrastructure.api.imagesearch.sub;

import com.ywf.ywfpicturebackend.infrastructure.api.imagesearch.ImageSearchResult;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class ImageSearchApiFacade {

    /**
     * 搜索图片
     *
     * @param imageUrl
     * @return
     */
    public static List<ImageSearchResult> searchImage(String imageUrl) {
        String imagePageUrl = GetImagePageUrlApi.getImagePageUrl(imageUrl);// 获取API地址保存登陆状态
        String imageFirstUrl = GetImageFirstUrlApi.getImageFirstUrl(imagePageUrl);// 调用API接口
        List<ImageSearchResult> imageList = GetImageListApi.getImageList(imageFirstUrl);
        return imageList;
    }

    public static void main(String[] args) {
        // 测试以图搜图功能
        String imageUrl = "https://www.codefather.cn/logo.png";
        List<ImageSearchResult> resultList = searchImage(imageUrl);
        System.out.println("结果列表" + resultList);
    }
}

