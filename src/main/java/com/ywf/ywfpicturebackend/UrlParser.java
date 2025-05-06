package com.ywf.ywfpicturebackend;

import java.net.URL;

public class UrlParser {
    public static void main(String[] args) throws Exception {
        String urlStr = "https://ywf-picture-backend-1317155406.cos.ap-guangzhou.myqcloud.com/space/1917558858494906369/2025-05-01_JxCAR5tmS7WnqNnE.png";
        URL url = new URL(urlStr);
        // URL.getPath() 会返回 "/space/1917558858494906369/2025-05-01_JxCAR5tmS7WnqNnE.png"
        String path = url.getPath();
        // 去掉开头的斜杠
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        System.out.println(path);
        // 输出: space/1917558858494906369/2025-05-01_JxCAR5tmS7WnqNnE.png
    }
}
