package meogajoa.chatAndGame.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class PersonalChatLog {
    private String id;
    private String sender;
    private String receiver;
    private String content;
    private LocalDateTime sendTime;

    public static PersonalChatLog of(String id, String sender, String receiver, String content, LocalDateTime sendTime){
        return PersonalChatLog.builder()
                .id(id)
                .sender(sender)
                .receiver(receiver)
                .content(content)
                .sendTime(sendTime)
                .build();
    }
}
