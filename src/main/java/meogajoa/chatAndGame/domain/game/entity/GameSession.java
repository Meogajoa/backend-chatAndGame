package meogajoa.chatAndGame.domain.game.entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import meogajoa.chatAndGame.common.dto.MeogajoaMessage;
import meogajoa.chatAndGame.common.model.MessageType;
import meogajoa.chatAndGame.domain.chat.dto.ChatLog;
import meogajoa.chatAndGame.domain.chat.dto.PersonalChatLog;
import meogajoa.chatAndGame.domain.game.listener.GameSessionListener;
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
        this.players = new Player[9];
        this.surviveCount = 8L;
        this.gameSessionListener = gameSessionListener;

        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.nicknameToPlayerNumber = nicknameToPlayerNumber;
        this.playerNumberToNickname = playerNumberToNickname;

        this.blackTeam = new ArrayList<>();
        this.whiteTeam = new ArrayList<>();
        this.eliminated = new ArrayList<>();

        this.personalChatLogMap = new HashMap<>();
        this.chatLogMap = new HashMap<>();
        this.spyColorMap = spyColorMap;

        for (int i = 1; i <= 3; i++) {
            this.whiteTeam.add((long) i);
        }

        for (int i = 4; i <= 6; i++) {
            this.blackTeam.add((long) i);
        }

        for (int i = 7; i <= 9; i++) {
            this.redTeam.add((long) i);
        }

        for (Player player : players) {
            this.players[player.getNumber().intValue()] = player;
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

    public void startGame() throws JsonProcessingException {
        //redisPubSubGameMessagePublisher.broadCastUserInfoList(players);
        redisPubSubGameMessagePublisher.broadCastDayNotice(id, 0L, "NIGHT");

        ZonedDateTime targetTime = ZonedDateTime.now(ZoneOffset.UTC).plusSeconds(20);

        redisPubSubGameMessagePublisher.broadCastMiniGameStartNotice(targetTime, MiniGameType.BUTTON_CLICK, id);
        this.miniGame = new ButtonGame(redisPubSubGameMessagePublisher, id);

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

        this.dayCount++;
        dayOrNight = "DAY";
        redisPubSubGameMessagePublisher.broadCastDayNotice(id, this.dayCount, dayOrNight);
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

        //this.miniGame.setBlind(true);
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

        gameSessionListener.onGameSessionEnd(id);


    }

    public void publishGameStatus() {
        redisPubSubGameMessagePublisher.broadCastDayNotice(id, dayCount, dayOrNight);
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

    public String findNicknameByPlayerNumber(Long playerNumber) {
        return playerNumberToNickname.get(playerNumber);
    }

    public Long findPlayerNumberByNickname(String nickname) {
        return nicknameToPlayerNumber.get(nickname);
    }

    public Boolean isBlackTeam(String sender) {
        Player player = players[nicknameToPlayerNumber.get(sender).intValue()];
        return player.getTeamColor().equals(TeamColor.BLACK);
    }

    public Boolean isWhiteTeam(String sender) {
        Player player = players[nicknameToPlayerNumber.get(sender).intValue()];
        return player.getTeamColor().equals(TeamColor.WHITE);
    }

    public boolean isEliminated(String sender) {
        Player player = players[nicknameToPlayerNumber.get(sender).intValue()];
        return player.isEliminated();
    }

    public void publishUserPersonalStatus(String nickname) {
        Player player = players[nicknameToPlayerNumber.get(nickname).intValue()];
        redisPubSubGameMessagePublisher.userInfoPersonal(player);
    }

    public MeogajoaMessage.GameUserListResponse getUserList() {
        return MeogajoaMessage.GameUserListResponse.builder()
                .type(MessageType.GAME_USER_LIST)
                .id(id)
                .blackTeam(blackTeam)
                .whiteTeam(whiteTeam)
                .eliminated(eliminated)
                .build();
    }


    public PersonalChatLog savePersonalChatLog(String content, Long sender, Long receiver) {
        String id = Math.min(sender, receiver) + ":" + Math.max(sender, receiver);

        PersonalChatLog personalChatLog = PersonalChatLog.builder()
                .id(id)
                .sender(sender.toString())
                .receiver(receiver.toString())
                .content(content)
                .sendTime(LocalDateTime.now())
                .build();

        personalChatLogMap.computeIfAbsent(id, k -> new ArrayList<>()).add(personalChatLog);

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
                .id(String.valueOf(playerNumber))
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("BLACK", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public ChatLog whiteChat(String content, Long playerNumber) {

        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(playerNumber))
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("WHITE", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public ChatLog eliminatedChat(String content, Long playerNumber) {
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(playerNumber))
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("ELIMINATED", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public ChatLog gameChat(String content, Long playerNumber) {
        ChatLog chatLog = ChatLog.builder()
                .id(String.valueOf(playerNumber))
                .content(content)
                .sender(playerNumber.toString())
                .sendTime(LocalDateTime.now())
                .build();

        chatLogMap.computeIfAbsent("GAME", k -> new ArrayList<>()).add(chatLog);

        return chatLog;
    }

    public List<ChatLog> getGameChatLogs() {
        return chatLogMap.get("GAME");
    }

    public List<ChatLog> getBlackChatLogs() {
        return chatLogMap.get("BLACK");
    }

    public List<ChatLog> getWhiteChatLogs() {
        return chatLogMap.get("WHITE");
    }

    public List<ChatLog> getEliminatedChatLogs() {
        return chatLogMap.get("ELIMINATED");
    }

    public List<ChatLog> getRedChatLogs() {
        return chatLogMap.get("RED");
    }
}
