package meogajoa.chatAndGame.domain.room.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import meogajoa.chatAndGame.domain.room.dto.RoomRequest;
import meogajoa.chatAndGame.domain.room.entity.Room;
import meogajoa.chatAndGame.domain.room.repository.CustomRedisRoomRepository;
import meogajoa.chatAndGame.domain.room.repository.RedisRoomRepository;
import meogajoa.chatAndGame.domain.session.entity.UserSession;
import meogajoa.chatAndGame.domain.session.repository.CustomRedisSessionRepository;
import meogajoa.chatAndGame.domain.session.repository.RedisSessionRepository;
import meogajoa.chatAndGame.domain.session.state.State;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
public class RoomService {

    private final CustomRedisRoomRepository customRedisRoomRepository;
    private final CustomRedisSessionRepository customRedisSessionRepository;
    private final RedisRoomRepository redisRoomRepository;
    private final RedisSessionRepository redisSessionRepository;
    private final RedissonClient redissonClient;
    private final String userSessionLockKey = "lock:userSession:";


    public String getRoomName(String id){
        return customRedisRoomRepository.getRoomName(id);
    }

    public String getRoomOwner(String roomId) {
        return customRedisRoomRepository.getRoomOwner(roomId);
    }


    public boolean isPlaying(String id) {
        return customRedisRoomRepository.isPlaying(id);
    }
}
