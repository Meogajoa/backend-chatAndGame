package meogajoa.chatAndGame.domain.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SyncStreamHandler {
    private final ObjectMapper objectMapper;

    private final GameSessionManager gameSessionManager;

    public SyncStreamHandler(ObjectMapper objectMapper, GameSessionManager gameSessionManager) throws InterruptedException {
        this.objectMapper = objectMapper;
        this.gameSessionManager = gameSessionManager;
    }

    public void handleMessage(MapRecord<String, String, String> record) {
        try {
            String type = record.getValue().get("type");

            switch (type) {
                case "GAME_START":
                    String gameId = record.getValue().get("gameId");
                    System.out.println("게임 시작 메시지 받음");
                    gameSessionManager.addGameSession(gameId);
                    break;
                case "ROOM_INFO":
                    break;
                case "TEST":
                    handleTest(record);
                    break;
                case "BUTTON_CLICK":{
                    MeogajoaMessage.GameMQRequest gameMQRequest = objectMapper.convertValue(record.getValue(), MeogajoaMessage.GameMQRequest.class);
                    gameSessionManager.addRequest(gameMQRequest);
                }

                default:
                    break;
            }

            log.info("sync - Current Thread: {}", Thread.currentThread().getName());
//            stringRedisTemplate.opsForStream().acknowledge(record.getStream(), GROUP_NAME, record.getId());
//            stringRedisTemplate.opsForStream().delete(record.getStream(), record.getId());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void handleTest(MapRecord<String, String, String> record) {
        MeogajoaMessage.GameMQRequest request = objectMapper.convertValue(record.getValue(), MeogajoaMessage.GameMQRequest.class);
        gameSessionManager.addRequest(request);
    }
}
