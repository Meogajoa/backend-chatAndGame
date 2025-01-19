package meogajoa.chatAndGame.domain.chat.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPubSubRoomChatPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    public void publish(Message.RoomChatPubSubResponse roomChatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(roomChatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:roomChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
