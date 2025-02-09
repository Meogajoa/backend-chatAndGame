package meogajoa.chatAndGame.domain.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLog;
import meogajoa.chatAndGame.domain.chat.publisher.RedisPubSubChatPublisher;
import meogajoa.chatAndGame.domain.chat.repository.CustomRedisChatLogRepository;
import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
import meogajoa.chatAndGame.domain.room.dto.RoomUserInfo;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AsyncStreamHandler {

    private final ObjectMapper objectMapper;
    private final RedisPubSubChatPublisher redisPubSubChatPublisher;
    private final CustomRedisChatLogRepository customRedisChatLogRepository;
    private final GameSessionManager gameSessionManager;


    public AsyncStreamHandler(ObjectMapper objectMapper, RedisPubSubChatPublisher redisPubSubChatPublisher, CustomRedisChatLogRepository customRedisChatLogRepository, GameSessionManager gameSessionManager) {
        this.objectMapper = objectMapper;
        this.redisPubSubChatPublisher = redisPubSubChatPublisher;
        this.customRedisChatLogRepository = customRedisChatLogRepository;
        this.gameSessionManager = gameSessionManager;
    }

    public void handleMessage(MapRecord<String, String, String> record) {
        try {
            String type = record.getValue().get("type");

            switch(type) {
                case "ROOM_INFO":
                    handleRoomInfoMessage(record);
                    break;
                case "ROOM_CHAT":
                    handleRoomChat(record);
                    break;
                case "GAME_CHAT":
                    handleGameChat(record);
                    break;
                case "GAME_CHAT_TO_USER":
                    handleGameChatToUser(record);
                    break;
                case "BLACK_CHAT":
                    handleBlackChat(record);
                    break;
                case "WHITE_CHAT":
                    handleWhiteChat(record);
                    break;
                case "ELIMINATED_CHAT":
                    handleEliminatedChat(record);
                    break;
                case "GAME_MY_INFO":{
                    String gameId = record.getValue().get("gameId");
                    String nickname = record.getValue().get("sender");
                    gameSessionManager.publishPersonalUserStatus(gameId, nickname);
                    break;
                }
                case "GAME_USER_LIST":{
                    String gameId = record.getValue().get("gameId");
                    System.out.println("게임 유저 리스트 메시지를 받았습니다.");
                    gameSessionManager.publishUserList(gameId);
                    break;
                }
                case "GAME_DAY_OR_NIGHT": {
                    String gameId = record.getValue().get("gameId");
                    gameSessionManager.publishGameStatus(gameId);
                    break;
                }
                case "GET_GAME_CHAT": {
                    String gameId = record.getValue().get("gameId");
                    String sender = record.getValue().get("sender");
                    System.out.println("받음 진입점");
                    gameSessionManager.publishGameChat(gameId, sender);
                    break;
                }
                default:
            }

            log.info("Async - Current Thread: {}", Thread.currentThread().getName());

//            stringRedisTemplate.opsForStream().acknowledge(record.getStream(), GROUP_NAME, record.getId());
//            stringRedisTemplate.opsForStream().delete(record.getStream(), record.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRoomInfoMessage(MapRecord<String, String, String> record) {
        try {
            String roomId = record.getValue().get("roomId");
            String users = record.getValue().get("users");

            RoomUserInfo roomUserInfo = objectMapper.readValue(users, RoomUserInfo.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleRoomChat(MapRecord<String, String, String> record) {
        try {
            String id = record.getValue().get("id");
            String content = record.getValue().get("content");
            String sender = record.getValue().get("sender");

            ChatLog chatLog = customRedisChatLogRepository.saveRoomChatLog(content, id, sender);

            MeogajoaMessage.ChatPubSubResponse chatPubSubResponse = MeogajoaMessage.ChatPubSubResponse.builder()
                    .id(id)
                    .chatLog(chatLog)
                    .build();

            redisPubSubChatPublisher.publishToRoom(chatPubSubResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleGameChat(MapRecord<String, String, String> record) {
        try {
            String id = record.getValue().get("id");
            String content = record.getValue().get("content");
            String sender = record.getValue().get("sender");

            gameSessionManager.gameChat(id, content, sender);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleGameChatToUser(MapRecord<String, String, String> record) {
        try {
            Long receiver = Long.parseLong(record.getValue().get("number"));
            String gameId = record.getValue().get("id");
            String content = record.getValue().get("content");

            if(!gameSessionManager.isGamePlaying(gameId)) return;

            Long sender = gameSessionManager.findPlayerNumberByNickname(gameId, record.getValue().get("sender"));

            PersonalChatLog personalChatLog = gameSessionManager.savePersonalChatLog(gameId, content, sender, receiver);

            MeogajoaMessage.ChatPubSubResponseToUser chatPubSubResponseToUser = MeogajoaMessage.ChatPubSubResponseToUser.builder()
                    .id(gameId)
                    .personalChatLog(personalChatLog)
                    .receiver(gameSessionManager.findNicknameByPlayerNumber(gameId, receiver))
                    .sender(gameSessionManager.findNicknameByPlayerNumber(gameId, sender))
                    .build();

            redisPubSubChatPublisher.publishGameChatToUser(chatPubSubResponseToUser);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleBlackChat(MapRecord<String, String, String> record) {
        try {
            String id = record.getValue().get("id");
            String content = record.getValue().get("content");
            String sender = record.getValue().get("sender");

            if(!gameSessionManager.isBlackTeam(id, sender)) return;

            Long senderNumber = gameSessionManager.findPlayerNumberByNickname(id, sender);
            ChatLog chatLog = customRedisChatLogRepository.saveBlackChatLog(content, id, senderNumber);



            MeogajoaMessage.ChatPubSubResponse chatPubSubResponse = MeogajoaMessage.ChatPubSubResponse.builder()
                    .id(id)
                    .chatLog(chatLog)
                    .build();

            redisPubSubChatPublisher.publishToBlack(chatPubSubResponse);


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleWhiteChat(MapRecord<String, String, String> record) {
        try {
            String id = record.getValue().get("id");
            String content = record.getValue().get("content");
            String sender = record.getValue().get("sender");

            if(!gameSessionManager.isWhiteTeam(id, sender)) return;

            Long senderNumber = gameSessionManager.findPlayerNumberByNickname(id, sender);
            ChatLog chatLog = customRedisChatLogRepository.saveWhiteChatLog(content, id, senderNumber);

            MeogajoaMessage.ChatPubSubResponse chatPubSubResponse = MeogajoaMessage.ChatPubSubResponse.builder()
                    .id(id)
                    .chatLog(chatLog)
                    .build();

            redisPubSubChatPublisher.publishToWhite(chatPubSubResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleEliminatedChat(MapRecord<String, String, String> record) {
        try {
            String id = record.getValue().get("id");
            String content = record.getValue().get("content");
            String sender = record.getValue().get("sender");

            if(!gameSessionManager.isEliminated(id, sender)) return;

            Long senderNumber = gameSessionManager.findPlayerNumberByNickname(id, sender);
            ChatLog chatLog = customRedisChatLogRepository.saveEliminatedChatLog(content, id, senderNumber);

            MeogajoaMessage.ChatPubSubResponse chatPubSubResponse = MeogajoaMessage.ChatPubSubResponse.builder()
                    .id(id)
                    .chatLog(chatLog)
                    .build();

            redisPubSubChatPublisher.publishToEliminated(chatPubSubResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
