package meogajoa.chatAndGame.domain.game.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.Message;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.game.entity.Player;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RedisPubSubGameMessagePublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final static String GAME_MESSAGE_KEY = "pubsub:gameStart";
    private final static String GAME_USER_INFO_KEY = "pubsub:userInfo";
    private final static String GAME_USER_INFO_PERSONAL_KEY = "pubsub:userInfoPersonal";
    private final static String GAME_DAY_OR_NIGHT_KEY = "pubsub:gameDayOrNight";

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

//    public void UserInfo(Player player){
//        try {
//            String jsonString = objectMapper.writeValueAsString(player);
//            stringRedisTemplate.convertAndSend(GAME_USER_INFO_PERSONAL_KEY, jsonString);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void broadCastDayNotice(String gameId, int day, String dayOrNight) {
        try {
            Message.GameDayOrNightResponse gameDayOrNightResponse = Message.GameDayOrNightResponse.builder()
                    .gameId(gameId)
                    .sender("SYSTEM")
                    .sendTime(LocalDateTime.now())
                    .type(MessageType.GAME_DAY_OR_NIGHT)
                    .day(day)
                    .dayOrNight(dayOrNight)
                    .build();

            String jsonString = objectMapper.writeValueAsString(gameDayOrNightResponse);

            stringRedisTemplate.convertAndSend(GAME_DAY_OR_NIGHT_KEY, jsonString);
            System.out.println("낮 밤 알림 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
