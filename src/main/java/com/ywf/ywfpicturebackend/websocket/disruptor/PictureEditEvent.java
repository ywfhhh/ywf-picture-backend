package com.ywf.ywfpicturebackend.websocket.disruptor;

import com.ywf.ywfpicturebackend.domain.user.entity.User;
import com.ywf.ywfpicturebackend.websocket.model.PictureEditRequestMessage;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

@Data
public class PictureEditEvent {

    /**
     * 消息
     */
    private PictureEditRequestMessage pictureEditRequestMessage;

    /**
     * 当前用户的 session
     */
    private WebSocketSession session;

    /**
     * 当前用户
     */
    private User user;

    /**
     * 图片 id
     */
    private Long pictureId;

}
