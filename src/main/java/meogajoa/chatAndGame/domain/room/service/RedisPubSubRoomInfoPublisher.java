package meogajoa.chatAndGame.domain.room.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import meogajoa.chatAndGame.domain.room.dto.RoomUserInfo;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPubSubRoomInfoPublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public void publishRoomInfo(RoomUserInfo roomUserInfo) {
        try {
            stringRedisTemplate.convertAndSend("pubsub:roomInfo", objectMapper.writeValueAsString(roomUserInfo));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
