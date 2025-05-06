package com.ywf.ywfpicturebackend.utils;

import org.apache.calcite.util.Static;

import java.net.MalformedURLException;
import java.net.URL;

public class UrlParserUtils {
    public static String getKeyFromUrl(String urlStr) throws MalformedURLException {
        URL url = new URL(urlStr);
        // URL.getPath() 会返回 "/space/1917558858494906369/2025-05-01_JxCAR5tmS7WnqNnE.png"
        String path = url.getPath();
        // 去掉开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        return path;
    }
}
