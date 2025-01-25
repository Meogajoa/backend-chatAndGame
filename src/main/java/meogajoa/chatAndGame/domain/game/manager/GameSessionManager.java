package meogajoa.chatAndGame.domain.game.manager;

import lombok.RequiredArgsConstructor;
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
    private final ThreadPoolTaskExecutor executor;
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;

    public GameSessionManager(@Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor executor, RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher) {
        this.executor = executor;
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
    }

    public void addGameSession(String gameId) {
        if(gameSessionMap.containsKey(gameId)){
            return;
        }

        GameSession gameSession = new GameSession(gameId, executor);
        gameSessionMap.put(gameId, gameSession);

        redisPubSubGameMessagePublisher.publish(gameId);
    }
}
