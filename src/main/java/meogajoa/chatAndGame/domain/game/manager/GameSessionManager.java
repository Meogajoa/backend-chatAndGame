package meogajoa.chatAndGame.domain.game.manager;

import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.game.entity.GameSession;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentHashMap;

@Service
public class GameSessionManager {
    private final ConcurrentHashMap<String, GameSession> gameSessionMap = new ConcurrentHashMap<>();
    private final ThreadPoolTaskExecutor executor;

    public GameSessionManager(@Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor executor) {
        this.executor = executor;
    }

    public void addGameSession(String gameId) {
        if(gameSessionMap.containsKey(gameId)){
            return;
        }

        GameSession gameSession = new GameSession(gameId, executor);
        gameSessionMap.put(gameId, gameSession);

        gameSession.startGame();
    }
}
