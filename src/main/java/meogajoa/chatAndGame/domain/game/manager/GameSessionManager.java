package meogajoa.chatAndGame.domain.game.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLog;
import meogajoa.chatAndGame.domain.chat.dto.ChatLogResponse;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLogResponse;
import meogajoa.chatAndGame.domain.chat.publisher.RedisPubSubChatPublisher;
import meogajoa.chatAndGame.domain.chat.repository.CustomRedisChatLogRepository;
import meogajoa.chatAndGame.domain.game.entity.GameSession;
import meogajoa.chatAndGame.domain.game.entity.Player;
import meogajoa.chatAndGame.domain.game.listener.GameSessionListener;
import meogajoa.chatAndGame.domain.game.model.TeamColor;
import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;
import meogajoa.chatAndGame.domain.room.entity.Room;
import meogajoa.chatAndGame.domain.room.repository.CustomRedisRoomRepository;
import meogajoa.chatAndGame.domain.room.repository.RedisRoomRepository;
import meogajoa.chatAndGame.domain.session.repository.CustomRedisSessionRepository;
import meogajoa.chatAndGame.domain.session.state.State;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GameSessionManager implements GameSessionListener {
    private final ConcurrentHashMap<String, GameSession> gameSessionMap = new ConcurrentHashMap<>();
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;
    private final CustomRedisSessionRepository customRedisSessionRepository;
    private final ThreadPoolTaskExecutor gameRunningExecutor;
    private final ThreadPoolTaskExecutor gameLogicExecutor;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedisRoomRepository redisRoomRepository;
    private final CustomRedisRoomRepository customRedisRoomRepository;
    private final CustomRedisChatLogRepository customRedisChatLogRepository;
    private final RedisPubSubChatPublisher redisPubSubChatPublisher;

    public GameSessionManager(
            @Qualifier("gameRunningExecutor") ThreadPoolTaskExecutor gameRunningExecutor,
            @Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor gameLogicExecutor,
            CustomRedisSessionRepository customRedisSessionRepository,
            RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher,
            StringRedisTemplate stringRedisTemplate, RedisRoomRepository redisRoomRepository,
            CustomRedisRoomRepository customRedisRoomRepository,
            RedisPubSubChatPublisher redisPubSubChatPublisher,
            CustomRedisChatLogRepository customRedisChatLogRepository) {
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.gameRunningExecutor = gameRunningExecutor;
        this.gameLogicExecutor = gameLogicExecutor;
        this.stringRedisTemplate = stringRedisTemplate;
        this.customRedisSessionRepository = customRedisSessionRepository;
        this.redisRoomRepository = redisRoomRepository;
        this.customRedisRoomRepository = customRedisRoomRepository;
        this.redisPubSubChatPublisher = redisPubSubChatPublisher;
        this.customRedisChatLogRepository = customRedisChatLogRepository;
    }

    public PersonalChatLog savePersonalChatLog(String gameId, String content, Long sender, Long receiver) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if(gameSession == null){
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        return gameSession.savePersonalChatLog(content, sender, receiver);

    }

    public void addGameSession(String gameId) throws InterruptedException {
        if (gameSessionMap.containsKey(gameId)) {
            System.out.println("이미 게임이 존재합니다.");
            return;
        }

        String nicknameKey = "room:" + gameId + ":users";
        Set<String> members = stringRedisTemplate.opsForSet().members(nicknameKey);
        stringRedisTemplate.opsForHash().entries("room:" + gameId);
        if (members == null || members.size() < 9) {
            System.out.println("게임 참가 인원이 부족합니다.");
            return;
        }

        Room room = redisRoomRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        AtomicLong playerNumberGenerator = new AtomicLong(1);
        room.setPlaying(true);
        redisRoomRepository.save(room);

        List<String> authorizations = customRedisRoomRepository.getUserSessionIdInRoom(gameId);
        for (String member : authorizations) {
            customRedisSessionRepository.setUserState(member, State.IN_GAME, gameId);
        }

        List<String> membersList = new ArrayList<>(members);
        Collections.shuffle(membersList);

        
        List<String> whiteTeamNicknames = membersList.subList(0, 3);
        List<String> blackTeamNicknames = membersList.subList(3, 6);
        List<String> redTeamNicknames = membersList.subList(6, 9);


        List<TeamColor> allTeams = Arrays.asList(TeamColor.RED, TeamColor.WHITE, TeamColor.BLACK);
        List<TeamColor> spyTeams = new ArrayList<>(allTeams);
        Collections.shuffle(spyTeams);
        spyTeams = spyTeams.subList(0, 2);
        System.out.println("선택된 스파이 팀: " + spyTeams);

        HashMap<TeamColor, TeamColor> spyMapping = new HashMap<>();
        if (spyTeams.size() == 2) {
            spyMapping.put(spyTeams.get(0), spyTeams.get(1));
            spyMapping.put(spyTeams.get(1), spyTeams.get(0));
        }

        int whiteSpyIndex = spyTeams.contains(TeamColor.WHITE) ? new Random().nextInt(3) : -1;
        int blackSpyIndex = spyTeams.contains(TeamColor.BLACK) ? new Random().nextInt(3) : -1;
        int redSpyIndex   = spyTeams.contains(TeamColor.RED)   ? new Random().nextInt(3) : -1;

        List<Player> players = new ArrayList<>();

        for (int i = 0; i < whiteTeamNicknames.size(); i++) {
            String nickname = whiteTeamNicknames.get(i);
            boolean isSpy = (i == whiteSpyIndex);
            Player player = new Player(
                    playerNumberGenerator.getAndIncrement(),
                    nickname,
                    TeamColor.WHITE,
                    1000L,
                    isSpy,
                    false
            );
            players.add(player);
        }

        for (int i = 0; i < blackTeamNicknames.size(); i++) {
            String nickname = blackTeamNicknames.get(i);
            boolean isSpy = (i == blackSpyIndex);
            Player player = new Player(
                    playerNumberGenerator.getAndIncrement(),
                    nickname,
                    TeamColor.BLACK,
                    1000L,
                    isSpy,
                    false
            );
            players.add(player);
        }

        for (int i = 0; i < redTeamNicknames.size(); i++) {
            String nickname = redTeamNicknames.get(i);
            boolean isSpy = (i == redSpyIndex);
            Player player = new Player(
                    playerNumberGenerator.getAndIncrement(),
                    nickname,
                    TeamColor.RED,
                    1000L,
                    isSpy,
                    false
            );
            players.add(player);
        }

        Map<String, Long> nicknameToPlayerNumber = new HashMap<>();
        Map<Long, String> playerNumberToNickname = new HashMap<>();
        for (Player player : players) {
            nicknameToPlayerNumber.put(player.getNickname(), player.getNumber());
            playerNumberToNickname.put(player.getNumber(), player.getNickname());
        }

        MeogajoaMessage.GameSystemResponse gameSystemResponse = MeogajoaMessage.GameSystemResponse.builder()
                .id(gameId)
                .sendTime(LocalDateTime.now())
                .type(MessageType.GAME_START)
                .content("게임을 시작해라!!!!!!!!")
                .sender("SYSTEM")
                .build();

        redisPubSubGameMessagePublisher.gameStart(gameSystemResponse);

        GameSession gameSession = new GameSession(
                gameId,
                gameLogicExecutor,
                players,
                redisPubSubGameMessagePublisher,
                nicknameToPlayerNumber,
                playerNumberToNickname,
                this,
                spyMapping
        );
        gameSessionMap.put(gameId, gameSession);

        gameRunningExecutor.submit(() -> {
            try {
                gameSession.startGame();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        redisPubSubGameMessagePublisher.userInfo(players);
    }

    public String findNicknameByPlayerNumber(String gameId, Long playerNumber) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        return gameSession.findNicknameByPlayerNumber(playerNumber);
    }

    public Long findPlayerNumberByNickname(String gameId, String nickname) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        return gameSession.findPlayerNumberByNickname(nickname);
    }

    public void addRequest(MeogajoaMessage.GameMQRequest request) {
        GameSession gameSession = gameSessionMap.get(request.getGameId());
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        gameSession.addRequest(request);
    }

    public void publishGameStatus(String gameId) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        gameSession.publishGameStatus();
    }

    public void publishUserStatus(String gameId, String nickname) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        gameSession.publishUserStatus(nickname);
    }

    @Override
    public void onGameSessionEnd(String gameId) {
        customRedisRoomRepository.getUserSessionIdInRoom(gameId).forEach(sessionId -> {
            customRedisSessionRepository.setUserState(sessionId, State.IN_ROOM, gameId);
        });

        Room room = redisRoomRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));
        room.setPlaying(false);
        redisRoomRepository.save(room);

        gameSessionMap.remove(gameId);

        redisPubSubGameMessagePublisher.publishGameEnd(gameId);
    }

    public boolean isGamePlaying(String gameId) {
        return gameSessionMap.containsKey(gameId);
    }

    public Boolean isBlackTeam(String id, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return false;
        }

        return gameSession.isBlackTeam(sender);
    }

    public Boolean isWhiteTeam(String id, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return false;
        }

        return gameSession.isWhiteTeam(sender);
    }

    public boolean isEliminated(String id, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return false;
        }

        return gameSession.isEliminated(sender);
    }

    public void publishPersonalUserStatus(String gameId, String nickname) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        gameSession.publishUserPersonalStatus(nickname);
    }


    public void publishUserList(String gameId) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        MeogajoaMessage.GameUserListResponse userListResponse = gameSession.getUserList();
        redisPubSubGameMessagePublisher.publishUserListInfo(userListResponse);
    }

    public void publishGameChat(String gameId, String sender) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        List<ChatLog> chatLog = gameSession.getGameChatLogs();
        ChatLogResponse chatLogResponse = ChatLogResponse.builder().type(MessageType.CHAT_LOGS).id(gameId).chatLogs(chatLog).build();
        redisPubSubChatPublisher.publishGameChatList(chatLogResponse);
    }

    public void publishPersonalChat(String gameId, String sender){
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        List<PersonalChatLog> personalChatLogs = gameSession.getPersonalChatLogs(sender);
        PersonalChatLogResponse personalChatLogResponse = PersonalChatLogResponse.builder().type(MessageType.PERSONAL_CHAT_LOGS).id(gameId).receiver(sender).personalChatLogs(personalChatLogs).build();
        redisPubSubChatPublisher.publishPersonalChatList(personalChatLogResponse);
    }

    public void publishBlackChat(String gameId, String sender) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        if(!gameSession.isBlackTeam(sender)){
            System.out.println("블랙팀이 아닙니다.");
            return;
        }

        List<ChatLog> chatLog = gameSession.getBlackChatLogs();
        ChatLogResponse chatLogResponse = ChatLogResponse.builder().type(MessageType.CHAT_LOGS).id(gameId).chatLogs(chatLog).build();
        redisPubSubChatPublisher.publishBlackChatList(chatLogResponse);
    }

    public void publishWhiteChat(String gameId, String sender) {
        GameSession gameSession = gameSessionMap.get(gameId);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        if(!gameSession.isWhiteTeam(sender)){
            System.out.println("화이트팀이 아닙니다.");
            return;
        }

        List<ChatLog> chatLog = gameSession.getWhiteChatLogs();
        ChatLogResponse chatLogResponse = ChatLogResponse.builder().type(MessageType.CHAT_LOGS).id(gameId).chatLogs(chatLog).build();
        redisPubSubChatPublisher.publishWhiteChatList(chatLogResponse);
    }

    public ChatLog gameChat(String id, String content, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        if(isEliminated(id, sender)){
            System.out.println("탈락한 유저입니다.");
            return null;
        }

        Long playerNumber = gameSession.findPlayerNumberByNickname(sender);

        return gameSession.gameChat(content, playerNumber);
    }

    public ChatLog blackChat(String id, String content, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        if(isEliminated(id, sender)){
            System.out.println("탈락한 유저입니다.");
            return null;
        }

        Long playerNumber = gameSession.findPlayerNumberByNickname(sender);

        return gameSession.blackChat(content, playerNumber);
    }

    public ChatLog whiteChat(String id, String content, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        if(isEliminated(id, sender)){
            System.out.println("탈락한 유저입니다.");
            return null;
        }

        Long playerNumber = gameSession.findPlayerNumberByNickname(sender);

        return gameSession.whiteChat(content, playerNumber);
    }

    public ChatLog eliminatedChat(String id, String content, String sender) {
        GameSession gameSession = gameSessionMap.get(id);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return null;
        }

        if(!isEliminated(id, sender)){
            System.out.println("탈락하지 않은 유저입니다.");
            return null;
        }

        Long playerNumber = gameSession.findPlayerNumberByNickname(sender);

        return gameSession.eliminatedChat(content, playerNumber);
    }

    public void publishEliminatedChat(String gameID, String sender) {
        GameSession gameSession = gameSessionMap.get(gameID);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        if(!gameSession.isEliminated(sender)){
            System.out.println("탈락하지 않은 유저입니다.");
            return;
        }

        List<ChatLog> chatLog = gameSession.getEliminatedChatLogs();
        ChatLogResponse chatLogResponse = ChatLogResponse.builder().type(MessageType.CHAT_LOGS).id(gameID).chatLogs(chatLog).build();
        redisPubSubChatPublisher.publishEliminatedChatList(chatLogResponse);

    }

    public void publishRedChat(String gameID, String sender) {
        GameSession gameSession = gameSessionMap.get(gameID);
        if (gameSession == null) {
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        if(!gameSession.isEliminated(sender)){
            System.out.println("탈락하지 않은 유저입니다.");
            return;
        }

        List<ChatLog> chatLog = gameSession.getRedChatLogs();
        ChatLogResponse chatLogResponse = ChatLogResponse.builder().type(MessageType.CHAT_LOGS).id(gameID).chatLogs(chatLog).build();
        redisPubSubChatPublisher.publishRedChatList(chatLogResponse);
    }
}