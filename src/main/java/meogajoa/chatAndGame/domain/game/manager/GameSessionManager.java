package meogajoa.chatAndGame.domain.game.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.game.entity.GameSession;
import meogajoa.chatAndGame.domain.game.entity.Player;
import meogajoa.chatAndGame.domain.game.model.TeamColor;
import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class GameSessionManager {
    private final ConcurrentHashMap<String, GameSession> gameSessionMap = new ConcurrentHashMap<>();
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;
    private final ThreadPoolTaskExecutor gameRunningExecutor;
    private final ThreadPoolTaskExecutor gameLogicExecutor;
    private final StringRedisTemplate stringRedisTemplate;
    private final AtomicLong playerNumberGenerator = new AtomicLong(1); // 플레이어 번호를 고유하게 할당하기 위한 제너레이터

    public GameSessionManager(
            @Qualifier("gameRunningExecutor") ThreadPoolTaskExecutor gameRunningExecutor,
            @Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor gameLogicExecutor,
            RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher,
            StringRedisTemplate stringRedisTemplate) {
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.gameRunningExecutor = gameRunningExecutor;
        this.gameLogicExecutor = gameLogicExecutor;
        this.stringRedisTemplate = stringRedisTemplate;
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

        GameSession gameSession = new GameSession(gameId, gameLogicExecutor, players, redisPubSubGameMessagePublisher);
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
}