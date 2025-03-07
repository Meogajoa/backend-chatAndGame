package meogajoa.chatAndGame.common.dto;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLog;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class MeogajoaMessage {

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
    public static class MiniGameNoticeResponse {
        private MessageType type;
        private String id;
        private String miniGameType;
        private String scheduledTime;
        private String sender;
        private LocalDateTime sendTime;
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

        public static RoomChatMQRequest of(MeogajoaMessage.Request request, String roomId, String sender) {
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
        private String id;
        private String sender;
        private String gameId;
        private MessageType type;
        private Long day;
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
    public static class ChatPubSubResponseToUser{
        String id;
        PersonalChatLog personalChatLog;
        String receiver;
        String sender;

        public static ChatPubSubResponseToUser of(String id, PersonalChatLog personalChatLog, String receiver, String sender) {
            return ChatPubSubResponseToUser.builder()
                    .id(id)
                    .personalChatLog(personalChatLog)
                    .receiver(receiver)
                    .sender(sender)
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
    public static class ButtonGameStatusResponse {
        private MessageType type;
        private String id;
        private String sender;
        private List<Long> twentyButtons;
        private List<Long> fiftyButtons;
        private List<Long> hundredButtons;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class VoteGameStatusResponse {
        private MessageType type;
        private String id;
        private String sender;
        private Map<String, Long> result;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class GameSystemResponse {
        private String sender;
        private String id;
        private MessageType type;
        private String content;
        private LocalDateTime sendTime;
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

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class GameEndResponse {
        private MessageType type;
        private String id;
        private String sender;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class GameUserListResponse {
        private MessageType type;
        private String id;
        private List<Long> blackTeam;
        private List<Long> whiteTeam;
        private List<Long> redTeam;
        private List<Long> eliminated;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class VoteResultResponse {
        private MessageType type;
        private String id;
        private String sender;
        private List<Long> eliminatedId;
        private Long surviveCount;
        private LocalDateTime sendTime;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class EliminatedUserResponse {
        private MessageType type;
        private String id;
        private String sender;
        private String nickname;
        private LocalDateTime sendTime;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class ReVoteNoticeResponse {
        private MessageType type;
        private String id;
        private String sender;
        private LocalDateTime sendTime;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class AvailableVoteCountResponse {
        private MessageType type;
        private String id;
        private String sender;
        private Long availableVoteCount;
        private String userNickname;
        private LocalDateTime sendTime;
    }
}