package meogajoa.chatAndGame.domain.chat.repository;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLog;
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
    private final static String GAME_CHAT_LOG_KEY = "chat_log:game:";
    private final static String CHAT_LOG_ID_KEY = "chat_log:id";
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public ChatLog saveRoomChatLog(String content, String roomId, String sender) {
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

    public ChatLog saveGameChatLog(String content, String gameId, String sender) {
        Long id = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(id))
                .content(content)
                .sender(sender)
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(GAME_CHAT_LOG_KEY + gameId, chatLog);

        return chatLog;
    }

    public ChatLog saveGameChatLog(String content, String gameId, Long receiver, Long sender){
        Long id = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(id))
                .content(content)
                .sender(sender.toString())
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(GAME_CHAT_LOG_KEY + gameId + ":to:" + receiver, chatLog);

        return chatLog;
    }

    public PersonalChatLog savePersonalChatLog(String gameId, String content, Long sender, Long receiver) {
        Long id = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        PersonalChatLog personalChatLog = PersonalChatLog.builder()
                .id(String.valueOf(id))
                .sender(sender.toString())
                .receiver(receiver.toString())
                .content(content)
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(GAME_CHAT_LOG_KEY + gameId + ":to:" + receiver, personalChatLog);

        return personalChatLog;

    }

    public List<ChatLog> getGameChatLog(String gameId) {
        List<ChatLog> chatLogs = new ArrayList<>();
        List<Object> chatLogList = redisTemplate.opsForList().range(GAME_CHAT_LOG_KEY + gameId, 0, -1);
        if(chatLogList == null) return chatLogs;
        for (Object chatLog : chatLogList) {
            chatLogs.add(objectMapper.convertValue(chatLog, ChatLog.class));
        }
        return chatLogs;
    }

    public List<ChatLog> getPersonalGameChatLog(String gameId, Long receiver) {
        List<ChatLog> chatLogs = new ArrayList<>();
        List<Object> chatLogList = redisTemplate.opsForList().range(GAME_CHAT_LOG_KEY + gameId + ":to:" + receiver, 0, -1);
        if(chatLogList == null) return chatLogs;
        for (Object chatLog : chatLogList) {
            chatLogs.add(objectMapper.convertValue(chatLog, ChatLog.class));
        }
        return chatLogs;
    }

    public List<ChatLog> getBlackChatLog(String id) {
        List<ChatLog> chatLogs = new ArrayList<>();
        List<Object> chatLogList = redisTemplate.opsForList().range(GAME_CHAT_LOG_KEY + id + ":black", 0, -1);
        if(chatLogList == null) return chatLogs;
        for (Object chatLog : chatLogList) {
            chatLogs.add(objectMapper.convertValue(chatLog, ChatLog.class));
        }
        return chatLogs;
    }

    public List<ChatLog> getWhiteChatLog(String id) {
        List<ChatLog> chatLogs = new ArrayList<>();
        List<Object> chatLogList = redisTemplate.opsForList().range(GAME_CHAT_LOG_KEY + id + ":white", 0, -1);
        if(chatLogList == null) return chatLogs;
        for (Object chatLog : chatLogList) {
            chatLogs.add(objectMapper.convertValue(chatLog, ChatLog.class));
        }
        return chatLogs;
    }

    public List<ChatLog> getEliminatedChatLog(String gameId) {
        List<ChatLog> chatLogs = new ArrayList<>();
        List<Object> chatLogList = redisTemplate.opsForList().range(GAME_CHAT_LOG_KEY + gameId + ":eliminated", 0, -1);
        if(chatLogList == null) return chatLogs;
        for (Object chatLog : chatLogList) {
            chatLogs.add(objectMapper.convertValue(chatLog, ChatLog.class));
        }
        return chatLogs;
    }

    public ChatLog saveBlackChatLog(String content, String id, Long senderNumber) {
        Long logId = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(logId))
                .content(content)
                .sender(senderNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(GAME_CHAT_LOG_KEY + id + ":black", chatLog);

        return chatLog;
    }

    public ChatLog saveWhiteChatLog(String content, String id, Long senderNumber) {
        Long logId = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(logId))
                .content(content)
                .sender(senderNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(GAME_CHAT_LOG_KEY + id + ":white", chatLog);

        return chatLog;
    }

    public ChatLog saveEliminatedChatLog(String content, String id, Long senderNumber) {
        Long logId = stringRedisTemplate.opsForValue().increment(CHAT_LOG_ID_KEY);
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(logId))
                .content(content)
                .sender(senderNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        redisTemplate.opsForList().rightPush(GAME_CHAT_LOG_KEY + id + ":eliminated", chatLog);

        return chatLog;
    }


    public void deleteRoomChatLog(String gameId) {
        redisTemplate.delete(GAME_CHAT_LOG_KEY + gameId);
    }
}
