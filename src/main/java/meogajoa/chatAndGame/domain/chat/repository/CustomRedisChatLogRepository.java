package meogajoa.chatAndGame.domain.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Repository
@RequiredArgsConstructor
public class CustomRedisChatLogRepository {
    private final StringRedisTemplate stringRedisTemplate;
    private final static String ROOM_CHAT_LOG_KEY = "chat_log:room:";
    private final static String CHAT_LOG_ID_KEY = "chat_log:id";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatLog saveChatLog(String content, String roomId, String sender) {
        Long id = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(id))
                .content(content)
                .sender(sender)
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(ROOM_CHAT_LOG_KEY + roomId, chatLog);

        return chatLog;
    }

    public List<ChatLog> getRoomChatLogs(String roomId) {
        List<Object> list = redisTemplate.opsForList().range(ROOM_CHAT_LOG_KEY + roomId, 0, -1);
        List<ChatLog> chatLogs = new ArrayList<ChatLog>();

        if(list == null) {
            return chatLogs;
        }

        for(Object o : list) {
            ChatLog chatLog = objectMapper.convertValue(o, ChatLog.class);
            chatLogs.add(chatLog);
        }

        return chatLogs;
    }
}
