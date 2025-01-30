package meogajoa.chatAndGame.domain.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.Message;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.publisher.RedisPubSubChatPublisher;
import meogajoa.chatAndGame.domain.chat.repository.CustomRedisChatLogRepository;
import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
import meogajoa.chatAndGame.domain.room.dto.RoomUserInfo;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncSubscriber {

    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final String ASYNC_STREAM_KEY = "stream:async:";
    private final String GROUP_NAME = "async-consumer-group";
    private final RedisPubSubChatPublisher redisPubSubChatPublisher;
    private final CustomRedisChatLogRepository customRedisChatLogRepository;
    private final GameSessionManager gameSessionManager;

    @PostConstruct
    public void startListening() {
        String streamKey = ASYNC_STREAM_KEY;

        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP_NAME);
        } catch (Exception e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                throw e;
            }
        }

        String consumerName = "asyncConsumer";

        Consumer consumer = Consumer.from(GROUP_NAME, consumerName);

        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        listenerContainer.receive(
                consumer,
                streamOffset,
                this::handleMessage
        );
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
                case "GAME_MY_INFO":{
                    String gameId = record.getValue().get("gameId");
                    String nickname = record.getValue().get("sender");
                    gameSessionManager.publishUserStatus(gameId, nickname);
                    break;
                }
                case "GAME_DAY_OR_NIGHT": {
                    String gameId = record.getValue().get("gameId");
                    gameSessionManager.publishGameStatus(gameId);
                    break;
                }
                default:
            }

            stringRedisTemplate.opsForStream().acknowledge(record.getStream(), GROUP_NAME, record.getId());
            stringRedisTemplate.opsForStream().delete(record.getStream(), record.getId());
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

            Message.ChatPubSubResponse chatPubSubResponse = Message.ChatPubSubResponse.builder()
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

            ChatLog chatLog = customRedisChatLogRepository.saveGameChatLog(content, id, sender);

            Message.ChatPubSubResponse chatPubSubResponse = Message.ChatPubSubResponse.builder()
                    .id(id)
                    .chatLog(chatLog)
                    .build();

            redisPubSubChatPublisher.publishToGame(chatPubSubResponse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
