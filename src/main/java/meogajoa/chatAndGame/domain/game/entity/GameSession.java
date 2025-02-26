package meogajoa.chatAndGame.domain.game.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLog;
import meogajoa.chatAndGame.domain.game.listener.GameSessionListener;
import meogajoa.chatAndGame.domain.game.listener.MiniGameListener;
import meogajoa.chatAndGame.domain.game.model.MiniGameType;
import meogajoa.chatAndGame.domain.game.model.TeamColor;
import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class GameSession implements MiniGameListener {
    private String id;
    private final ThreadPoolTaskExecutor executor;
    private final LinkedBlockingQueue<MeogajoaMessage.GameMQRequest> requestQueue = new LinkedBlockingQueue<>();
    private Long dayCount;
    private String dayOrNight;
    private MiniGame miniGame;
    private Long surviveCount;
    private List<Long> blackTeam;
    private List<Long> whiteTeam;
    private List<Long> redTeam;
    private List<Long> eliminated;
    private Map<String, Long> nicknameToPlayerNumber;
    private Map<Long, String> playerNumberToNickname;
    private Player[] players;
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;
    private final GameSessionListener gameSessionListener;
    private Map<String, List<PersonalChatLog>> personalChatLogMap;
    private Map<String, List<ChatLog>> chatLogMap;
    private Map<TeamColor, TeamColor> spyColorMap;


    private AtomicBoolean processing = new AtomicBoolean(false);

    private volatile boolean isGameRunning = true;

    public GameSession(String id, @Qualifier("gameLogicExecutor") ThreadPoolTaskExecutor executor, List<Player> players, RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher, Map<String, Long> nicknameToPlayerNumber, Map<Long, String> playerNumberToNickname, GameSessionListener gameSessionListener, Map<TeamColor, TeamColor> spyColorMap) {
        this.id = id;
        this.executor = executor;
        this.dayCount = 0L;
        this.dayOrNight = "NIGHT";
        this.players = new Player[11];
        this.surviveCount = 8L;
        this.gameSessionListener = gameSessionListener;

        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.nicknameToPlayerNumber = nicknameToPlayerNumber;
        this.playerNumberToNickname = playerNumberToNickname;

        this.blackTeam = new ArrayList<>();
        this.whiteTeam = new ArrayList<>();
        this.redTeam = new ArrayList<>();
        this.eliminated = new ArrayList<>();

        this.personalChatLogMap = new HashMap<>();
        this.chatLogMap = new HashMap<>();
        this.spyColorMap = spyColorMap;

        for (Player player : players) {
            this.players[player.getNumber().intValue()] = player;
            TeamColor teamcolor = player.getTeamColor();
            switch (teamcolor) {
                case BLACK -> blackTeam.add(player.getNumber());
                case WHITE -> whiteTeam.add(player.getNumber());
                case RED -> redTeam.add(player.getNumber());
            }
        }
    }

    public Long getPlayerNumberByNickname(String nickname) {
        return nicknameToPlayerNumber.get(nickname);
    }

    public void addRequest(MeogajoaMessage.GameMQRequest request) {
        requestQueue.add(request);
        triggerProcessingIfNeeded();
    }

    public void triggerProcessingIfNeeded() {
        if (processing.compareAndSet(false, true)) {
            executor.execute(this::processRequest);
        }
    }

    private void processRequest() {
        try {
            while (isGameRunning) {
                MeogajoaMessage.GameMQRequest request = requestQueue.poll(500, TimeUnit.MILLISECONDS);
                if (request == null) {
                    break;
                }

                if (request.getType().equals(MessageType.BUTTON_CLICK) && this.miniGame instanceof ButtonGame) {
                    handleButtonClickRequest(request);
                }

                if(request.getType().equals(MessageType.VOTE) && this.miniGame instanceof VoteGame){
                    handleVoteRequest(request);
                }

                if(request.getType().equals(MessageType.CANCEL_VOTE) && this.miniGame instanceof VoteGame){
                    handleCancelVoteRequest(request);
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            processing.set(false);

            if (isGameRunning && !requestQueue.isEmpty()) {
                triggerProcessingIfNeeded();
            }
        }
    }

    private void handleVoteRequest(MeogajoaMessage.GameMQRequest request) {
        this.miniGame.clickButton(nicknameToPlayerNumber.get(request.getSender()), request.getContent());
    }

    private void handleCancelVoteRequest(MeogajoaMessage.GameMQRequest request) {
        this.miniGame.cancelButton(nicknameToPlayerNumber.get(request.getSender()), Long.valueOf(request.getContent()));
    }

    private void handleButtonClickRequest(MeogajoaMessage.GameMQRequest request) {
        switch (request.getContent()) {
            case "twenty" ->
                    this.miniGame.clickButton(nicknameToPlayerNumber.get(request.getSender()), request.getContent());
            case "fifty" ->
                    this.miniGame.clickButton(nicknameToPlayerNumber.get(request.getSender()), request.getContent());
            case "hundred" ->
                    this.miniGame.clickButton(nicknameToPlayerNumber.get(request.getSender()), request.getContent());
            default -> System.out.println("잘못된 버튼 클릭입니다.");
        }
    }

    private void handleRequest(MeogajoaMessage.GameMQRequest request) {
        if (request.getType() == MessageType.TEST) {
            System.out.println(LocalDateTime.now() + "에 보냈어요!!!: ");
        }
    }

    public void stopSession() {
        isGameRunning = false;
        requestQueue.clear();
    }

    public void goToTheNext() {
        if(dayOrNight.equals("NIGHT")){
            dayCount++;
            dayOrNight = "DAY";
            redisPubSubGameMessagePublisher.broadCastDayOrNightNotice(id, dayCount, dayOrNight);
        } else {
            dayOrNight = "NIGHT";
            redisPubSubGameMessagePublisher.broadCastDayOrNightNotice(id, dayCount, dayOrNight);
        }
    }



    public void startGame() throws JsonProcessingException {
        //redisPubSubGameMessagePublisher.broadCastUserInfoList(players);
        redisPubSubGameMessagePublisher.broadCastDayOrNightNotice(id, dayCount, dayOrNight);

        ZonedDateTime targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(20);

        redisPubSubGameMessagePublisher.broadCastMiniGameStartNotice(targetTime, MiniGameType.BUTTON_CLICK, id);

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

        this.miniGame = new ButtonGame(redisPubSubGameMessagePublisher, id);

//        this.dayCount++;
//        dayOrNight = "DAY";
//        redisPubSubGameMessagePublisher.broadCastDayNotice(id, this.dayCount, dayOrNight);
        goToTheNext();
        redisPubSubGameMessagePublisher.broadCastMiniGameEndNotice(targetTime, MiniGameType.BUTTON_CLICK, id);

        targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(30);
        ZonedDateTime blindTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(20);
        this.miniGame.publishCurrentStatus();

        while (true) {
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            if (!now.isBefore(blindTime)) {
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        this.miniGame.setBlind(true);
        this.miniGame.publishCurrentStatus();

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

        targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(5);
        redisPubSubGameMessagePublisher.broadCastMiniGameStartNotice(targetTime, MiniGameType.VOTE_GAME, id);

        while(true){
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            if(!now.isBefore(targetTime)){
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        goToTheNext();
        List<Long> candidates = new ArrayList<>();
        for (int i = 1; i <= 9; i++) {
            if(players[i].isEliminated()) continue;
            candidates.add((long) i);
        }
        this.miniGame = new VoteGame(candidates, redisPubSubGameMessagePublisher, id, playerNumberToNickname);
        targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(40);


        redisPubSubGameMessagePublisher.broadCastMiniGameEndNotice(targetTime, MiniGameType.VOTE_GAME, id);
        boolean revote = false;

        while(true){
            ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
            if(!now.isBefore(targetTime)){
                break;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }


        List<Long> preliminaryEliminated = new ArrayList<>();
        if (miniGame instanceof VoteGame) {
            preliminaryEliminated = ((VoteGame) miniGame).checkVoteResult();
        }

        if(preliminaryEliminated.size() == 1){
            players[preliminaryEliminated.getFirst().intValue()].eliminate();
            eliminated.add(preliminaryEliminated.getFirst());
            surviveCount--;
            List<Long> eliminatedList = new ArrayList<>();
            eliminatedList.add(preliminaryEliminated.getFirst());
            List<String> eliminatedNicknames = new ArrayList<>();
            eliminatedNicknames.add(players[preliminaryEliminated.getFirst().intValue()].getNickname());
            redisPubSubGameMessagePublisher.broadCastEliminatedNotice(id, eliminatedList, surviveCount);

            for(String nickname : eliminatedNicknames){
                redisPubSubGameMessagePublisher.publishEliminatedNicknames(id, nickname);
            }
        }else if(preliminaryEliminated.isEmpty()){

        }else{
            revote = true;
        }

        if(revote){
            targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(10);
            redisPubSubGameMessagePublisher.broadCastMiniGameStartNotice(targetTime, MiniGameType.RE_VOTE_GAME, id);
            this.miniGame.publishCurrentStatus();

            while(true){
                ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
                if(!now.isBefore(targetTime)){
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(40);
            this.miniGame = new VoteGame(preliminaryEliminated, redisPubSubGameMessagePublisher, id, playerNumberToNickname);
            redisPubSubGameMessagePublisher.broadCastMiniGameEndNotice(targetTime, MiniGameType.RE_VOTE_GAME, id);
            this.miniGame.publishCurrentStatus();

            while(true){
                ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
                if(!now.isBefore(targetTime)){
                    break;
                }

                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }


            List<Long> lastEliminated = new ArrayList<>();
            if (miniGame instanceof VoteGame) {
                lastEliminated = ((VoteGame) miniGame).checkVoteResult();
            }

            List<Long> lastEliminatedList = new ArrayList<>();
            List<String> lastEliminatedNicknames = new ArrayList<>();
            for(Long eliminated : lastEliminated){
                players[eliminated.intValue()].eliminate();
                surviveCount--;
                lastEliminatedList.add(eliminated);
                lastEliminatedNicknames.add(players[eliminated.intValue()].getNickname());
            }

            redisPubSubGameMessagePublisher.broadCastEliminatedNotice(id, lastEliminatedList, surviveCount);
            for(String nickname : lastEliminatedNicknames){
                redisPubSubGameMessagePublisher.publishEliminatedNicknames(id, nickname);
            }
        }

        if(surviveCount <= 3){
            gameSessionListener.onGameSessionEnd(id);
        }

        revote = false;

        gameSessionListener.onGameSessionEnd(id);


    }

    public void publishGameStatus() {
        redisPubSubGameMessagePublisher.broadCastDayOrNightNotice(id, dayCount, dayOrNight);
    }

    public void publishUserStatus(String nickname) {
        List<Player> temp = new ArrayList<>();
        for (int i = 1; i <= 8; i++) {
            if (players[i].getNickname().equals(nickname)) {
                temp.add(players[i]);
                redisPubSubGameMessagePublisher.userInfo(temp);
                System.out.println("유저 List에 포함시켜서 날렸음");
                break;
            }
        }
    }

    @Override
    public String findNicknameByPlayerNumber(Long playerNumber) {
        return playerNumberToNickname.get(playerNumber);
    }

    @Override
    public Long findPlayerNumberByNickname(String nickname) {
        return nicknameToPlayerNumber.get(nickname);
    }

    public Boolean isBlackTeam(String sender) {
        Long number = nicknameToPlayerNumber.get(sender);
        if(number == null) return false;
        Player player = players[number.intValue()];
        return player.getTeamColor().equals(TeamColor.BLACK);
    }

    public Boolean isWhiteTeam(String sender) {
        Long number = nicknameToPlayerNumber.get(sender);
        if(number == null) return false;
        Player player = players[number.intValue()];
        return player.getTeamColor().equals(TeamColor.WHITE);
    }

    public boolean isEliminated(String sender) {
        Long number = nicknameToPlayerNumber.get(sender);
        if(number == null) return false;
        Player player = players[number.intValue()];
        return player.isEliminated();
    }

    public void publishUserPersonalStatus(String nickname) {
        Long number = nicknameToPlayerNumber.get(nickname);
        if(number == null) return;
        Player player = players[number.intValue()];
        redisPubSubGameMessagePublisher.userInfoPersonal(player);
    }

    public MeogajoaMessage.GameUserListResponse getUserList() {
        return MeogajoaMessage.GameUserListResponse.builder()
                .type(MessageType.GAME_USER_LIST)
                .id(id)
                .blackTeam(blackTeam)
                .whiteTeam(whiteTeam)
                .redTeam(redTeam)
                .eliminated(eliminated)
                .build();
    }


    public PersonalChatLog savePersonalChatLog(String content, Long sender, Long receiver) {
        String key = Math.min(sender, receiver) + ":" + Math.max(sender, receiver);

        PersonalChatLog personalChatLog = PersonalChatLog.builder()
                .id(UUID.randomUUID().toString())
                .sender(sender.toString())
                .receiver(receiver.toString())
                .content(content)
                .sendTime(LocalDateTime.now())
                .build();

        personalChatLogMap.computeIfAbsent(key, k -> new ArrayList<>()).add(personalChatLog);

        return personalChatLog;
    }

    public List<PersonalChatLog> getPersonalChatLogs(String sender) {
        List<PersonalChatLog> personalChatLogs = new ArrayList<>();
        Long number = nicknameToPlayerNumber.get(sender);
        for(int i = 1; i <= 8; i++) {
            if(i == number) continue;
            String id = Math.min(i, number) + ":" + Math.max(i, number);
            List<PersonalChatLog> personalChatLogList = personalChatLogMap.get(id);
            if(personalChatLogList != null) {
                personalChatLogs.addAll(personalChatLogList);
            }
        }

        return personalChatLogs;
    }

    public ChatLog blackChat(String content, Long playerNumber) {
        ChatLog chatLog = ChatLog.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("BLACK", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public ChatLog whiteChat(String content, Long playerNumber) {

        ChatLog chatLog = ChatLog.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("WHITE", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public ChatLog eliminatedChat(String content, Long playerNumber) {
        ChatLog chatLog = ChatLog.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("ELIMINATED", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public ChatLog gameChat(String content, Long playerNumber) {
        ChatLog chatLog = ChatLog.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("GAME", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public List<ChatLog> getGameChatLogs() {
        List<ChatLog> chatLogs = chatLogMap.get("GAME");
        if(chatLogs == null) chatLogs = new ArrayList<>();
        return chatLogs;
    }

    public List<ChatLog> getBlackChatLogs() {
        List<ChatLog> chatLogs = chatLogMap.get("BLACK");
        if(chatLogs == null) chatLogs = new ArrayList<>();
        return chatLogs;
        //return chatLogMap.get("BLACK");
    }

    public List<ChatLog> getWhiteChatLogs() {
        List<ChatLog> chatLogs = chatLogMap.get("WHITE");
        if(chatLogs == null) chatLogs = new ArrayList<>();
        return chatLogs;
    }

    public List<ChatLog> getEliminatedChatLogs() {
        List<ChatLog> chatLogs = chatLogMap.get("ELIMINATED");
        if(chatLogs == null) chatLogs = new ArrayList<>();
        return chatLogs;
    }

    public List<ChatLog> getRedChatLogs() {
        List<ChatLog> chatLogs = chatLogMap.get("RED");
        if(chatLogs == null) chatLogs = new ArrayList<>();
        return chatLogs;
    }

    public ChatLog redChat(String content, Long playerNumber) {
        ChatLog chatLog = ChatLog.builder()
                .id(UUID.randomUUID().toString())
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("RED", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public boolean isRedTeam(String sender) {
        Long number = nicknameToPlayerNumber.get(sender);
        if(number == null) return false;
        Player player = players[number.intValue()];
        return player.getTeamColor().equals(TeamColor.RED);
    }

    @Override
    public void onGameEnd() {

    }
}
