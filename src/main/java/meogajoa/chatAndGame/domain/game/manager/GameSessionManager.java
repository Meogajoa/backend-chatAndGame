package meogajoa.chatAndGame.domain.game.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
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

    public GameSessionManager(
            @Qualifier("gameRunningExecutor") ThreadPoolTaskExecutor gameRunningExecutor,
            @Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor gameLogicExecutor,
            CustomRedisSessionRepository customRedisSessionRepository,
            RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher,
            StringRedisTemplate stringRedisTemplate, RedisRoomRepository redisRoomRepository, CustomRedisRoomRepository customRedisRoomRepository) {
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.gameRunningExecutor = gameRunningExecutor;
        this.gameLogicExecutor = gameLogicExecutor;
        this.stringRedisTemplate = stringRedisTemplate;
        this.customRedisSessionRepository = customRedisSessionRepository;
        this.redisRoomRepository = redisRoomRepository;
        this.customRedisRoomRepository = customRedisRoomRepository;
    }

    public void addGameSession(String gameId) throws InterruptedException {
        if (gameSessionMap.containsKey(gameId)) {
            System.out.println("이미 게임이 존재합니다.");
            return;
        }

        String nicknameKey = "room:" + gameId + ":users";

        Set<String> members = stringRedisTemplate.opsForSet().members(nicknameKey);
        stringRedisTemplate.opsForHash().entries("room:" + gameId);
        if (members == null || members.size() < 8) {
            System.out.println("게임 참가 인원이 부족합니다.");
            return;
        }

        Room room = redisRoomRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 방입니다."));

        AtomicLong playerNumberGenerator = new AtomicLong(1);

        room.setPlaying(true);
        redisRoomRepository.save(room);

        List<String> authorizations = customRedisRoomRepository.getUserSessionIdInRoom(gameId);

        for(String member : authorizations) {
            customRedisSessionRepository.setUserState(member, State.IN_GAME, gameId);
        }


        List<String> membersList = new ArrayList<>(members);
        Collections.shuffle(membersList);

        List<String> whiteTeamNicknames = membersList.subList(0, 4);
        List<String> blackTeamNicknames = membersList.subList(4, 8);

        int whiteSpyIndex = new Random().nextInt(4);
        int blackSpyIndex = new Random().nextInt(4);

        List<Player> players = new ArrayList<>();

        for (int i = 0; i < whiteTeamNicknames.size(); i++) {
            String nickname = whiteTeamNicknames.get(i);
            boolean isSpy = (i == whiteSpyIndex);
            Player player = new Player(
                    playerNumberGenerator.getAndIncrement(),
                    nickname,
                    TeamColor.WHITE,
                    1000L,
                    isSpy
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
                    isSpy
            );
            players.add(player);
        }

        Map<String, Long> nicknameToPlayerNumber = new HashMap<>();
        for (Player player : players) {
            nicknameToPlayerNumber.put(player.getNickname(), player.getNumber());
        }

        GameSession gameSession = new GameSession(gameId, gameLogicExecutor, players, redisPubSubGameMessagePublisher, nicknameToPlayerNumber, this);
        gameSessionMap.put(gameId, gameSession);

        gameRunningExecutor.submit(() -> {
            try {
                gameSession.startGame();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        redisPubSubGameMessagePublisher.UserInfo(players);

        MeogajoaMessage.GameSystemResponse gameSystemResponse = MeogajoaMessage.GameSystemResponse.builder()
                .sendTime(LocalDateTime.now())
                .type(MessageType.GAME_START)
                .content(gameId)
                .sender("SYSTEM")
                .build();

        redisPubSubGameMessagePublisher.gameStart(gameSystemResponse);
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
}