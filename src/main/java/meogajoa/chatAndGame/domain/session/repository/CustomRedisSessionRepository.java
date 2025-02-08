package meogajoa.chatAndGame.domain.session.repository;

import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.session.entity.UserSession;
import meogajoa.chatAndGame.domain.session.state.State;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;

@RequiredArgsConstructor
@Repository
public class CustomRedisSessionRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;

    private final String SESSION_PREFIX = "session:";
    private final String NICKNAME_TO_SESSIONID_PREFIX = "nicknameToSessionId:";

    public boolean isValidSessionId(String sessionId) {
        try {
            Boolean exists = stringRedisTemplate.hasKey(SESSION_PREFIX + sessionId);
            if(!exists) {
                System.out.println("세션아이디가 유효하지 않다 1/4");
            }

            return exists;
        } catch (Exception e) {
            System.out.println("Redis 예외 발생: " + e.getMessage());
            return false;
        }
    }

    public void saveSessionId(String sessionId, String email) {
        stringRedisTemplate.opsForValue().set(sessionId, email);
    }

    public void deleteSessionId(String sessionId) {
        if(sessionId == null) return;
        Boolean result = stringRedisTemplate.delete(sessionId);
    }

    public UserSession trackUserSession(String sessionId) {
        String key = SESSION_PREFIX + sessionId;
        Map<Object, Object> sessionMap = stringRedisTemplate.opsForHash().entries(key);

        return UserSession.of(
                (String) sessionMap.get("nickname"),
                State.valueOf((String) sessionMap.get("state")),
                (String) sessionMap.get("sessionId"),
                (String) sessionMap.get("roomId"),
                sessionMap.get("isInGame") != null && Boolean.parseBoolean(sessionMap.get("isInGame").toString()),
                sessionMap.get("isInRoom") != null && Boolean.parseBoolean(sessionMap.get("isInRoom").toString())
        );
    }

    public void setUserState(String authorization, State state, String roomId) {
        stringRedisTemplate.opsForHash().put(SESSION_PREFIX + authorization, "state", state.toString());
        stringRedisTemplate.opsForHash().put(SESSION_PREFIX + authorization, "roomId", roomId);
        stringRedisTemplate.opsForHash().put(SESSION_PREFIX + authorization, "isInRoom", String.valueOf(true));
    }

    public String getNicknameBySessionId(String authorization) {
        return (String) stringRedisTemplate.opsForHash().get(SESSION_PREFIX + authorization, "nickname");
    }

    public void saveNicknameToSession(String nickname, String sessionId) {
        stringRedisTemplate.opsForValue().set(NICKNAME_TO_SESSIONID_PREFIX + nickname, sessionId);
    }

    public String getNicknameToSessionId(String nickname) {
        return stringRedisTemplate.opsForValue().get(NICKNAME_TO_SESSIONID_PREFIX + nickname);
    }

    public boolean isUserSessionActive(String nickname) {
        return stringRedisTemplate.hasKey(NICKNAME_TO_SESSIONID_PREFIX + nickname);
    }

    public String getSessionIdByNickname(String nickname) {
        return stringRedisTemplate.opsForValue().get(NICKNAME_TO_SESSIONID_PREFIX + nickname);
    }

    public String getRoomIdBySessionId(String authorization) {
        return stringRedisTemplate.opsForHash().get(SESSION_PREFIX + authorization, "roomId").toString();
    }

    public String getRoomIdNickname(String nickname) {
        return stringRedisTemplate.opsForHash().get(NICKNAME_TO_SESSIONID_PREFIX + nickname, "roomId").toString();
    }
}
