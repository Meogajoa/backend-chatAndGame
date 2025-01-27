package meogajoa.chatAndGame.domain.game.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.game.entity.Player;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisPubSubGameMessagePublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final static String GAME_MESSAGE_KEY = "pubsub:gameStart";
    private final static String GAME_USER_INFO_KEY = "pubsub:userInfo";

    public void gameStart(String gameId) {
        try {
            stringRedisTemplate.convertAndSend(GAME_MESSAGE_KEY, gameId);
            System.out.println("게임 시작 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void UserInfo(List<Player> players) {
        try {
            String jsonString = objectMapper.writeValueAsString(players);
            stringRedisTemplate.convertAndSend(GAME_USER_INFO_KEY, jsonString);
            System.out.println("유저 정보 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
