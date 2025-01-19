package meogajoa.chatAndGame.domain.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.Message;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.publisher.RedisPubSubRoomChatPublisher;
import meogajoa.chatAndGame.domain.chat.repository.CustomRedisChatLogRepository;
import meogajoa.chatAndGame.domain.room.dto.RoomUserInfo;
import meogajoa.chatAndGame.domain.room.publisher.RedisPubSubRoomInfoPublisher;
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
    private final String GROUP_NAME = "consumer-group";
    private final RedisPubSubRoomChatPublisher redisPubSubRoomChatPublisher;
    private final RedisPubSubRoomInfoPublisher redisPubSubRoomInfoPublisher;
    private final CustomRedisChatLogRepository customRedisChatLogRepository;

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

        String consumerName = "Consumer";

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
                    handleRoomChat(record);;
                    break;
                default:
                    System.out.println("Unknown type: " + type);
            }
        } catch (Exception e) {
            System.err.println("Error processing message: " + e.getMessage());
            e.printStackTrace();
        }

    }

    public void handleRoomInfoMessage(MapRecord<String, String, String> record) {
        try {
            String roomId = record.getValue().get("roomId");
            String users = record.getValue().get("users");

            RoomUserInfo roomUserInfo = objectMapper.readValue(users, RoomUserInfo.class);

            stringRedisTemplate.opsForStream().acknowledge(record.getStream(), GROUP_NAME, record.getId());
            stringRedisTemplate.opsForStream().delete(record.getStream(), record.getId());
        } catch (Exception e) {
            System.err.println("Error processing room info message: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void handleRoomChat(MapRecord<String, String, String> record) {
        try {
            String roomId = record.getValue().get("roomId");
            String content = record.getValue().get("content");
            String sender = record.getValue().get("sender");

            ChatLog chatLog = customRedisChatLogRepository.saveChatLog(content, roomId, sender);

            Message.RoomChatPubSubResponse roomChatPubSubResponse = Message.RoomChatPubSubResponse.builder()
                    .id(roomId)
                    .chatLog(chatLog)
                    .build();

            redisPubSubRoomChatPublisher.publish(roomChatPubSubResponse);

            stringRedisTemplate.opsForStream().acknowledge(record.getStream(), GROUP_NAME, record.getId());
            stringRedisTemplate.opsForStream().delete(record.getStream(), record.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
