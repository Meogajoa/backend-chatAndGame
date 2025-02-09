package meogajoa.chatAndGame.domain.chat.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import meogajoa.chatAndGame.common.model.MessageType;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class PersonalChatLogResponse {
    private MessageType type;
    private String id;
    private String receiver;
    private List<PersonalChatLog> personalChatLogs;
}
