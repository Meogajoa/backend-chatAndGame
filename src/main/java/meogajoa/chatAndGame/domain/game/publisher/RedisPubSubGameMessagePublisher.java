package meogajoa.chatAndGame.domain.game.publisher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.game.entity.Player;
import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
import meogajoa.chatAndGame.domain.game.model.MiniGameType;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisPubSubGameMessagePublisher {
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;
    private final static String GAME_START_MESSAGE_KEY = "pubsub:gameStart";
    private final static String GAME_END_MESSAGE_KEY = "pubsub:gameEnd";
    private final static String GAME_USER_INFO_KEY = "pubsub:userInfo";
    private final static String GAME_USER_INFO_PERSONAL_KEY = "pubsub:gameUserInfoPersonal";
    private final static String GAME_DAY_OR_NIGHT_KEY = "pubsub:gameDayOrNight";
    private final static String GAME_MINI_GAME_NOTICE_KEY = "pubsub:miniGameNotice";
    private final static String BUTTON_GAME_STATUS_KEY = "pubsub:buttonGameStatus";
    private final static String GAME_USER_LIST_INFO_KEY = "pubsub:gameUserListInfo";
    private final static String VOTE_GAME_STATUS_KEY = "pubsub:voteGameStatus";
    private final static String VOTE_RESULT_KEY = "pubsub:voteResult";
    private final static String ELIMINATED_USER_KEY = "pubsub:eliminatedUser";
    private final static String RE_VOTE_NOTICE_KEY = "pubsub:reVoteNotice";
    private final static String AVAILABLE_VOTE_COUNT_KEY = "pubsub:availableVoteCount";

    public void gameStart(MeogajoaMessage.GameSystemResponse gameSystemResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(gameSystemResponse);
            stringRedisTemplate.convertAndSend(GAME_START_MESSAGE_KEY, jsonString);
            System.out.println("게임 시작 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void userInfo(List<Player> players) {
        try {
            String jsonString = objectMapper.writeValueAsString(players);
            stringRedisTemplate.convertAndSend(GAME_USER_INFO_KEY, jsonString);
            System.out.println("유저 정보 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    public void UserInfo(Player player){
//        try {
//            String jsonString = objectMapper.writeValueAsString(player);
//            stringRedisTemplate.convertAndSend(GAME_USER_INFO_PERSONAL_KEY, jsonString);
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public void broadCastDayOrNightNotice(String gameId, Long day, String dayOrNight) {
        try {
            MeogajoaMessage.GameDayOrNightResponse gameDayOrNightResponse = MeogajoaMessage.GameDayOrNightResponse.builder()
                    .id(gameId)
                    .sender("SYSTEM")
                    .sendTime(LocalDateTime.now())
                    .type(MessageType.GAME_DAY_OR_NIGHT)
                    .day(day)
                    .dayOrNight(dayOrNight)
                    .build();

            String jsonString = objectMapper.writeValueAsString(gameDayOrNightResponse);

            stringRedisTemplate.convertAndSend(GAME_DAY_OR_NIGHT_KEY, jsonString);
            System.out.println("낮 밤 알림 메시지를 보냈습니다.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void broadCastMiniGameStartNotice(ZonedDateTime targetTime, MiniGameType miniGameType, String id) throws JsonProcessingException {
        MeogajoaMessage.MiniGameNoticeResponse miniGameNoticeResponse = MeogajoaMessage.MiniGameNoticeResponse.builder()
                .id(id)
                .type(MessageType.MINI_GAME_WILL_START_NOTICE)
                .miniGameType(miniGameType.name())
                .scheduledTime(targetTime.toString())
                .sender("SYSTEM")
                .sendTime(LocalDateTime.now())
                .build();

        String jsonString = objectMapper.writeValueAsString(miniGameNoticeResponse);

        stringRedisTemplate.convertAndSend(GAME_MINI_GAME_NOTICE_KEY, jsonString);
    }

    public void broadCastMiniGameEndNotice(ZonedDateTime targetTime, MiniGameType miniGameType, String id) throws JsonProcessingException {
        MeogajoaMessage.MiniGameNoticeResponse miniGameNoticeResponse = MeogajoaMessage.MiniGameNoticeResponse.builder()
                .id(id)
                .type(MessageType.MINI_GAME_WILL_END_NOTICE)
                .miniGameType(miniGameType.name())
                .scheduledTime(targetTime.toString())
                .sender("SYSTEM")
                .sendTime(LocalDateTime.now())
                .build();

        String jsonString = objectMapper.writeValueAsString(miniGameNoticeResponse);

        stringRedisTemplate.convertAndSend(GAME_MINI_GAME_NOTICE_KEY, jsonString);
    }

    public void publishButtonGameStatus(List<Long> twentyButtons, List<Long> fiftyButtons, List<Long> hundredButtons, String gameId) {
        try {
            MeogajoaMessage.ButtonGameStatusResponse buttonGameStatusResponse = MeogajoaMessage.ButtonGameStatusResponse.builder()
                    .type(MessageType.BUTTON_GAME_STATUS)
                    .id(gameId)
                    .sender("SYSTEM")
                    .twentyButtons(twentyButtons)
                    .fiftyButtons(fiftyButtons)
                    .hundredButtons(hundredButtons)
                    .build();

            String jsonString = objectMapper.writeValueAsString(buttonGameStatusResponse);

            stringRedisTemplate.convertAndSend(BUTTON_GAME_STATUS_KEY, jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void publishGameEnd(String gameId) {
        try{
            MeogajoaMessage.GameSystemResponse gameSystemResponse = MeogajoaMessage.GameSystemResponse.builder()
                    .id(gameId)
                    .sendTime(LocalDateTime.now())
                    .type(MessageType.GAME_END)
                    .content("게임이 종료되었습니다.")
                    .sender("SYSTEM")
                    .build();

            String jsonString = objectMapper.writeValueAsString(gameSystemResponse);

            stringRedisTemplate.convertAndSend(GAME_END_MESSAGE_KEY, jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void publishUserListInfo(MeogajoaMessage.GameUserListResponse userListResponse) {
        try {
            String jsonString = objectMapper.writeValueAsString(userListResponse);
            stringRedisTemplate.convertAndSend(GAME_USER_LIST_INFO_KEY, jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void userInfoPersonal(Player player) {
        try{
            String jsonString = objectMapper.writeValueAsString(player);
            stringRedisTemplate.convertAndSend(GAME_USER_INFO_PERSONAL_KEY, jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void publishVoteGameStatus(String id, Map<String, Long> result) {
        try {
            MeogajoaMessage.VoteGameStatusResponse voteGameStatusResponse = MeogajoaMessage.VoteGameStatusResponse.builder()
                    .type(MessageType.VOTE_GAME_STATUS)
                    .id(id)
                    .sender("SYSTEM")
                    .result(result)
                    .build();

            String jsonString = objectMapper.writeValueAsString(voteGameStatusResponse);

            stringRedisTemplate.convertAndSend(VOTE_GAME_STATUS_KEY, jsonString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    public void broadCastEliminatedNotice(String id, List<Long> eliminatedList, Long surviveCount) {
        try {
            MeogajoaMessage.VoteResultResponse voteResultResponse = MeogajoaMessage.VoteResultResponse.builder()
                    .id(id)
                    .type(MessageType.VOTE_RESULT)
                    .sender("SYSTEM")
                    .eliminatedId(eliminatedList)
                    .surviveCount(surviveCount)
                    .sendTime(LocalDateTime.now())
                    .build();

            String jsonString = objectMapper.writeValueAsString(voteResultResponse);

            stringRedisTemplate.convertAndSend(VOTE_RESULT_KEY, jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void publishEliminatedNicknames(String id, String nickname) {
        try {
            MeogajoaMessage.EliminatedUserResponse eliminatedUserResponse = MeogajoaMessage.EliminatedUserResponse.builder()
                    .id(id)
                    .type(MessageType.ELIMINATED_USER)
                    .sender("SYSTEM")
                    .nickname(nickname)
                    .sendTime(LocalDateTime.now())
                    .build();

            String jsonString = objectMapper.writeValueAsString(eliminatedUserResponse);

            stringRedisTemplate.convertAndSend(ELIMINATED_USER_KEY, jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void broadCastReVoteNotice(String id) {
        try {
            MeogajoaMessage.ReVoteNoticeResponse reVoteNoticeResponse = MeogajoaMessage.ReVoteNoticeResponse.builder()
                    .id(id)
                    .type(MessageType.RE_VOTE_NOTICE)
                    .sender("SYSTEM")
                    .sendTime(LocalDateTime.now())
                    .build();

            String jsonString = objectMapper.writeValueAsString(reVoteNoticeResponse);

            stringRedisTemplate.convertAndSend(RE_VOTE_NOTICE_KEY, jsonString);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    public void publishAvailableVoteCount(String gameId, String userNickname, Long availableVoteCount) {
        try {
            MeogajoaMessage.AvailableVoteCountResponse availableVoteCountResponse = MeogajoaMessage.AvailableVoteCountResponse.builder()
                    .id(UUID.randomUUID().toString())
                    .type(MessageType.AVAILABLE_VOTE_COUNT)
                    .sender("SYSTEM")
                    .userNickname(userNickname)
                    .availableVoteCount(availableVoteCount)
                    .sendTime(LocalDateTime.now())
                    .build();

            String jsonString = objectMapper.writeValueAsString(availableVoteCountResponse);

            stringRedisTemplate.convertAndSend(AVAILABLE_VOTE_COUNT_KEY, jsonString);
        } catch (JsonProcessingException e){
            e.printStackTrace();
        }
    }
}
