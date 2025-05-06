package com.ywf.ywfpicturebackend.utils;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Iterator;

public class ImageUtils {
    public static boolean validExtName(String extName) {
        return Arrays.asList("jpg", "png", "jpeg", "gif", "bmp", "tiff", "webp").contains(extName);
    }

    public static String getFileExtension(File file) throws IOException {
        String format = getImageFormat(file);
        if (format == null) {
            return null;
        }
        // 转换为小写，便于比较
        format = format.toLowerCase();
        // 根据格式返回对应的扩展名
        switch (format) {
            case "jpeg":
            case "jpg":
                return "jpg";
            case "png":
                return "png";
            case "gif":
                return "gif";
            case "bmp":
                return "bmp";
            case "tiff":
                return "tiff";
            case "webp":
                return "webp";
            default:
                return format;
        }
    }

    public static String getImageFormat(File file) throws IOException {// 创建图片输入流
        ImageInputStream iis = ImageIO.createImageInputStream(file);
        // 获取所有 ImageReader
        Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
        if (!readers.hasNext()) {
            iis.close();
            return null;
        }
        // 获取第-个 ImageReader
        ImageReader reader = readers.next();
        String formatName = reader.getFormatName();
        // 关闭资源
        iis.close();
        reader.dispose();
        return formatName;
    }
}
