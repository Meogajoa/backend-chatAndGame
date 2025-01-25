package meogajoa.chatAndGame.domain.game.manager;

import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.common.dto.Message;
import meogajoa.chatAndGame.domain.game.entity.GameSession;
import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {
    private final ConcurrentHashMap<String, GameSession> gameSessionMap = new ConcurrentHashMap<>();
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;
    private final ThreadPoolTaskExecutor gameRunningExecutor;
    private final ThreadPoolTaskExecutor gameLogicExecutor;

    public GameSessionManager(@Qualifier("gameRunningExecutor") ThreadPoolTaskExecutor gameRunningExecutor,@Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor gameLogicExecutor, RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher) {
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.gameRunningExecutor = gameRunningExecutor;
        this.gameLogicExecutor = gameLogicExecutor;
    }

    public void addGameSession(String gameId) throws InterruptedException {
        if(gameSessionMap.containsKey(gameId)){
            System.out.println("이미 게임이 존재합니다.");
            return;
        }

        GameSession gameSession = new GameSession(gameId, gameLogicExecutor);
        gameSessionMap.put(gameId, gameSession);

        gameRunningExecutor.submit(() -> {
            try {
                gameSession.startGame();
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        });

        redisPubSubGameMessagePublisher.publish(gameId);
    }

    public void addRequest(Message.GameMQRequest request){
        GameSession gameSession = gameSessionMap.get(request.getGameId());
        if(gameSession == null){
            System.out.println("게임이 존재하지 않습니다.");
            return;
        }

        gameSession.addRequest(request);
    }
}
