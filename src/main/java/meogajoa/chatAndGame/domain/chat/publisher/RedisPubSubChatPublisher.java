package meogajoa.chatAndGame.domain.chat.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPubSubChatPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    public void publishToRoom(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:roomChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishToGame(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:gameChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
