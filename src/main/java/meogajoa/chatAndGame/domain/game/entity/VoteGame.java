package meogajoa.chatAndGame.domain.game.entity;

import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

public class VoteGame implements MiniGame {
    private String id;
    private final Map<Long, List<Long>> voteCount;
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;
    private final Map<Long, AtomicLong> availableVoteCount;
    private final AtomicLong zeroCount = new AtomicLong(0);
    private Map<Long, String> playerNumberToNickname;

    public VoteGame(List<Long> candidates, RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher, String id, Map<Long, String> playerNumberToNickname) {
        this.id = id;
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        this.playerNumberToNickname = playerNumberToNickname;
        voteCount = new HashMap<>();
        for (Long candidate : candidates) {
            voteCount.put(candidate, new ArrayList<>());
        }

        this.availableVoteCount = new HashMap<>();
        for(long i = 1L; i <= 9L; i++){
            availableVoteCount.put(i, new AtomicLong(1));
        }

        publishCurrentStatus();
    }

    @Override
    public void publishCurrentStatus() {
        Map<String, Long> result = new HashMap<>();

        for (Map.Entry<Long, List<Long>> entry : voteCount.entrySet()) {
            result.put(entry.getKey().toString(), (long) entry.getValue().size());
        }

        redisPubSubGameMessagePublisher.publishVoteGameStatus(id, result);

        for(long i = 1L; i <= 9; i++){
            redisPubSubGameMessagePublisher.publishAvailableVoteCount(id, playerNumberToNickname.get(i), availableVoteCount.get(i).get());
            //System.out.println("플레이어 " + playerNumberToNickname.get(i) + "의 투표권: " + availableVoteCount.get(i).get());
        }

        System.out.println("현재 투표 현황: " + result);
    }

    @Override
    public void clickButton(Long userId, String button) {
        Long candidateId = Long.parseLong(button);

        List<Long> list = voteCount.get(userId);

        if(availableVoteCount.get(userId).get() == 0){
            System.out.println("투표권이 없습니다.");
            return;
        }

        if (voteCount.containsKey(candidateId)) {
            voteCount.get(candidateId).add(userId);
            availableVoteCount.get(userId).decrementAndGet();
        } else {
            System.out.println("잘못된 후보 번호입니다.");
        }

        publishCurrentStatus();
    }

    @Override
    public void setBlind(boolean isBlind) {

    }

    @Override
    public void startGame() {

    }

    @Override
    public void endGame() {

    }

    @Override
    public void cancelButton(Long userId, Long target) {
        System.out.println("VOTEGAME 취소 시도");
        List<Long> voteListOfTarget = voteCount.get(target);
        for(Long voter : voteListOfTarget){
            if(voter.equals(userId)) {
                voteListOfTarget.remove(voter);
                availableVoteCount.get(userId).incrementAndGet();
                break;
            }
        }


        publishCurrentStatus();
    }


    public List<Long> checkVoteResult() {

        Map<Long, Integer> counts = new HashMap<>();
        for (Map.Entry<Long, List<Long>> entry : voteCount.entrySet()) {
            counts.put(entry.getKey(), entry.getValue().size());
        }


        int maxVotes = counts.values().stream()
                .filter(v -> v > 0)
                .max(Integer::compare)
                .orElse(0);

        if (maxVotes == 0) {
            return new ArrayList<>();
        }

        return counts.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

}
