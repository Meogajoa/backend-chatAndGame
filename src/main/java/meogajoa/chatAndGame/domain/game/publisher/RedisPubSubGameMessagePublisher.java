package meogajoa.chatAndGame.domain.game.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.Message;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPubSubGameMessagePublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final static String GAME_MESSAGE_KEY = "pubsub:gameStart";

    public void publish(String gameId) {
        try {
            stringRedisTemplate.convertAndSend(GAME_MESSAGE_KEY, gameId);
            System.out.println("게임 시작 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
