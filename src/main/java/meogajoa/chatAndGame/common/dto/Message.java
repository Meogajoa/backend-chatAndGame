package meogajoa.chatAndGame.common.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;

import java.time.LocalDateTime;

public class Message {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class Request {
        private MessageType type;
        private String content;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RoomChatMQRequest {
        private MessageType type;
        private String roomId;
        private String content;
        private String sender;

        public static RoomChatMQRequest of(Message.Request request, String roomId, String sender) {
            return RoomChatMQRequest.builder()
                    .type(request.getType())
                    .roomId(roomId)
                    .content(request.getContent())
                    .sender(sender)
                    .build();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class GameDayOrNightResponse {
        private String gameId;
        private String sender;
        private MessageType type;
        private int day;
        private String dayOrNight;
        private LocalDateTime sendTime;
    }


    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ChatPubSubResponse {
        String id;
        ChatLog chatLog;

        public static ChatPubSubResponse of(String id, ChatLog chatLog) {
            return ChatPubSubResponse.builder()
                    .id(id)
                    .chatLog(chatLog)
                    .build();
        }
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class GameMQRequest {
        private MessageType type;
        private String gameId;
        private String sender;
        private String content;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class GameStartPubSubResponse {
        private MessageType type;
        private String gameId;
        private String content;
    }
}