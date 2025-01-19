package meogajoa.chatAndGame.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;
import jakarta.persistence.Id;
import java.time.LocalDateTime;

@RedisHash(value = "chat_log")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
public class ChatLog {

    @Id
    private String id;

    private String content;

    @Indexed
    private String sender;

    private LocalDateTime sendTime;

    public static ChatLog of(String id, String content, String sender, LocalDateTime sendTime){
        return ChatLog.builder()
                .id(id)
                .content(content)
                .sender(sender)
                .sendTime(sendTime)
                .build();
    }


}