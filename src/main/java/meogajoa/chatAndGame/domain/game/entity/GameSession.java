package meogajoa.chatAndGame.domain.game.entity;

import meogajoa.chatAndGame.common.dto.Message;
import meogajoa.chatAndGame.common.model.MessageType;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSession {
    private final String gameId;
    private final ThreadPoolTaskExecutor executor;
    private final LinkedBlockingQueue<Message.GameMQRequest> requestQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean processing = new AtomicBoolean(false);

    private volatile boolean isGameRunning = true;

    public GameSession(String gameId, @Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor executor) {
        this.gameId = gameId;
        this.executor = executor;
    }

    public void addRequest(Message.GameMQRequest request){
        requestQueue.add(request);
        triggerProcessingIfNeeded();
    }

    public void triggerProcessingIfNeeded(){
        if(processing.compareAndSet(false, true)){
            executor.execute(this::processRequest);
        }
    }

    private void processRequest(){
        try{
            while(isGameRunning){
                Message.GameMQRequest request = requestQueue.poll(500, TimeUnit.MILLISECONDS);
                if(request == null) {
                    break;
                }

                handleRequest(request);

            }
        } catch (InterruptedException e){
            Thread.currentThread().interrupt();
        } finally {
            processing.set(false);

            if(isGameRunning && !requestQueue.isEmpty()){
                triggerProcessingIfNeeded();
            }
        }
    }

    private void handleRequest(Message.GameMQRequest request){
        if(request.getType() == MessageType.TEST){
            System.out.println(LocalDateTime.now() + "에 보냈어요!!!: ");
        }
    }

    public void stopSession() {
        isGameRunning = false;
        requestQueue.clear();
    }

    public void startGame() throws InterruptedException {
        // Game logic
        for(int i = 1; i <= 10; i++){
            System.out.println("게임 진행 중: " + i);
            Thread.sleep(1000);
        }
    }
}
