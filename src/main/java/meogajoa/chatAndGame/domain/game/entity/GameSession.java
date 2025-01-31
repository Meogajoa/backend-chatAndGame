package meogajoa.chatAndGame.domain.game.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.game.model.MiniGameType;
import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSession {
    private final String id;
    private final ThreadPoolTaskExecutor executor;
    private final LinkedBlockingQueue<MeogajoaMessage.GameMQRequest> requestQueue = new LinkedBlockingQueue<>();
    private Long dayCount;
    private String dayOrNight;
    private MiniGame miniGame;
    private Long surviveCount;
    private List<Long> blackTeam;
    private List<Long> whiteTeam;
    private Player[] players;
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;


    private final AtomicBoolean processing = new AtomicBoolean(false);

    private volatile boolean isGameRunning = true;

    public GameSession(String id, @Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor executor, List<Player> players, RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher) {
        this.id = id;
        this.executor = executor;
        this.dayCount = 0L;
        this.dayOrNight = "NIGHT";
        this.players = new Player[9];
        this.surviveCount = 8L;

        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;

        int idx = 1;

        for(Player player : players) {
            this.players[idx] = player;
            idx++;
        }

    }

    public void addRequest(MeogajoaMessage.GameMQRequest request){
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
                MeogajoaMessage.GameMQRequest request = requestQueue.poll(500, TimeUnit.MILLISECONDS);
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

    private void handleRequest(MeogajoaMessage.GameMQRequest request){
        if(request.getType() == MessageType.TEST){
            System.out.println(LocalDateTime.now() + "에 보냈어요!!!: ");
        }
    }

    public void stopSession() {
        isGameRunning = false;
        requestQueue.clear();
    }

    public void startGame() throws JsonProcessingException {
        redisPubSubGameMessagePublisher.broadCastDayNotice(id, 0, "NIGHT");

        ZonedDateTime targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(20);

        redisPubSubGameMessagePublisher.broadCastNextMiniGameNotice(targetTime, MiniGameType.BUTTON_CLICK, id);

        while (true) {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            if (!now.isBefore(targetTime)) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        //startMinigame(MiniGame.);
    }

    public void miniGameAlert(){


    }

    public void publishGameStatus() {
        redisPubSubGameMessagePublisher.broadCastDayNotice(id, dayCount.intValue(), dayOrNight);
    }

    public void publishUserStatus(String nickname) {
        List<Player> temp = new ArrayList<>();
        for(int i = 1; i <= 8; i++){
            if(players[i].getNickname().equals(nickname)){
                temp.add(players[i]);
                redisPubSubGameMessagePublisher.UserInfo(temp);
                System.out.println("유저 List에 포함시켜서 날렸음");
                break;
            }
        }
    }
}
