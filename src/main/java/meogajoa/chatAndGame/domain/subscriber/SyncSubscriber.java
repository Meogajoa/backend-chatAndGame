//package meogajoa.chatAndGame.domain.subscriber;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
//import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
//import org.springframework.data.redis.connection.stream.Consumer;
//import org.springframework.data.redis.connection.stream.MapRecord;
//import org.springframework.data.redis.connection.stream.ReadOffset;
//import org.springframework.data.redis.connection.stream.StreamOffset;
//import org.springframework.data.redis.core.StringRedisTemplate;
//import org.springframework.data.redis.stream.StreamMessageListenerContainer;
//import org.springframework.stereotype.Service;
//
//@Service
//@RequiredArgsConstructor
//@Slf4j
//public class SyncSubscriber {
//    private final StreamMessageListenerContainer<String, MapRecord<String, String, String>> listenerContainer;
//    private final StringRedisTemplate stringRedisTemplate;
//    private final ObjectMapper objectMapper;
//    private final String SYNC_STREAM_KEY = "stream:sync:";
//    private final String GROUP_NAME = "sync-consumer-group";
//
//    private final GameSessionManager gameSessionManager;
//
//    @PostConstruct
//    public void startListening() {
//        String streamKey = SYNC_STREAM_KEY;
//
//        try {
//            stringRedisTemplate.opsForStream().createGroup(streamKey, GROUP_NAME);
//        } catch (Exception e) {
//            if (!e.getMessage().contains("BUSYGROUP")) {
//                throw e;
//            }
//        }
//
//        String consumerName = "syncConsumer";
//
//        Consumer consumer = Consumer.from(GROUP_NAME, consumerName);
//
//        StreamOffset<String> streamOffset = StreamOffset.create(streamKey, ReadOffset.lastConsumed());
//
//        listenerContainer.receive(
//                consumer,
//                streamOffset,
//                this::handleMessage
//        );
//    }
//
//    private void handleMessage(MapRecord<String, String, String> record) {
//        try {
//            String type = record.getValue().get("type");
//
//            switch (type) {
//                case "GAME_START":
//                    String gameId = record.getValue().get("gameId");
//                    System.out.println("게임 시작 메시지 받음");
//                    gameSessionManager.addGameSession(gameId);
//                    break;
//                case "ROOM_INFO":
//                    break;
//                case "TEST":
//                    handleTest(record);
//                    break;
//                default:
//                    break;
//            }
//
//            log.info("sync - Current Thread: {}", Thread.currentThread().getName());
//            stringRedisTemplate.opsForStream().acknowledge(record.getStream(), GROUP_NAME, record.getId());
//            stringRedisTemplate.opsForStream().delete(record.getStream(), record.getId());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//
//
//    }
//
//    public void handleTest(MapRecord<String, String, String> record) {
//        MeogajoaMessage.GameMQRequest request = objectMapper.convertValue(record.getValue(), MeogajoaMessage.GameMQRequest.class);
//        gameSessionManager.addRequest(request);
//    }
//
//
//
//
//}
