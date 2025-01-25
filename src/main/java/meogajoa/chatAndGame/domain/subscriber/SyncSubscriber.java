package meogajoa.chatAndGame.domain.subscriber;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.chat.publisher.RedisPubSubRoomChatPublisher;
import meogajoa.chatAndGame.domain.chat.repository.CustomRedisChatLogRepository;
import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.stereotype.Service;

import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReentrantLock;

@Service
@RequiredArgsConstructor
public class SyncSubscriber {
    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
    private final StringRedisTemplate stringRedisTemplate;
    private final String SYNC_STREAM_KEY = "stream:sync:";
    private final String GROUP_NAME = "sync-consumer-group";

    private final GameSessionManager gameSessionManager;

    @PostConstruct
    public void startListening() {
        String streamKey = SYNC_STREAM_KEY;

        try {
            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP_NAME);
        } catch (Exception e) {
            if (!e.getMessage().contains("BUSYGROUP")) {
                throw e;
            }
        }

        String consumerName = "syncConsumer";

        Consumer consumer = Consumer.from(GROUP_NAME, consumerName);

        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());

        listenerContainer.receive(
                consumer,
                streamOffset,
                this::handleMessage
        );
    }

    private void handleMessage(MapRecord<String, String, String> record) {
        try {
            String type = record.getValue().get("type");

            switch (type) {
                case "GAME_START":
                    String gameId = record.getValue().get("gameId");
                    gameSessionManager.addGameSession(gameId);
                    break;
                case "ROOM_INFO":
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }




}
