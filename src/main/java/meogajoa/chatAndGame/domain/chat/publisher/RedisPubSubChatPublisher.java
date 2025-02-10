package meogajoa.chatAndGame.domain.chat.publisher;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLogResponse;
import meogajoa.chatAndGame.domain.chat.dto.ChatLogResponse;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisPubSubChatPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    public void publishToRoom(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:roomChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishToGame(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            System.out.println("게임챗 중개WAS로 보냄");
            stringRedisTemplate.convertAndSend("pubsub:gameChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishGameChatToUser(MeogajoaMessage.ChatPubSubResponseToUser chatPubSubResponseToUser) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponseToUser);
            stringRedisTemplate.convertAndSend("pubsub:gameChatToUser", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishToBlack(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:blackChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishToWhite(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:whiteChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishToEliminated(MeogajoaMessage.ChatPubSubResponse chatPubSubResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatPubSubResponse);

            stringRedisTemplate.convertAndSend("pubsub:eliminatedChat", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishGameChatList(ChatLogResponse chatLogResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(chatLogResponse);

            stringRedisTemplate.convertAndSend("pubsub:gameChatList", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishPersonalChatList(PersonalChatLogResponse personalChatLogResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(personalChatLogResponse);

            stringRedisTemplate.convertAndSend("pubsub:personalChatList", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishBlackChatList(ChatLogResponse blackChatLogResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(blackChatLogResponse);

            stringRedisTemplate.convertAndSend("pubsub:blackChatList", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishWhiteChatList(ChatLogResponse whiteChatLogResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(whiteChatLogResponse);

            stringRedisTemplate.convertAndSend("pubsub:whiteChatList", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void publishEliminatedChatList(ChatLogResponse eliminatedChatLogResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(eliminatedChatLogResponse);

            stringRedisTemplate.convertAndSend("pubsub:eliminatedChatList", jsonString);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
