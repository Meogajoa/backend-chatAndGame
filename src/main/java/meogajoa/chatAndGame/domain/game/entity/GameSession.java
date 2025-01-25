package meogajoa.chatAndGame.domain.game.entity;

import meogajoa.chatAndGame.common.dto.Message;
import net.bytebuddy.dynamic.scaffold.MethodGraph;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSession {
    private final String gameId;
    private final ThreadPoolTaskExecutor executor;
    private final LinkedBlockingQueue<Message.GameMQRequest> requestQueue = new LinkedBlockingQueue<>();

    private final AtomicBoolean processing = new AtomicBoolean(false);

    private volatile boolean isGameRunning = true;

    public GameSession(String gameId, ThreadPoolTaskExecutor executor) {
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

    }

    public void stopSession() {
        isGameRunning = false;
        requestQueue.clear();
    }

    @Async("gameRunningExecutor")
    public void startGame() {
    }
}
