package meogajoa.chatAndGame.domain.game.entity;

import meogajoa.chatAndGame.domain.game.publisher.RedisPubSubGameMessagePublisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class VoteGame implements MiniGame {
    private String id;
    private final Map<Long, List<Long>> voteCount;
    private final RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher;

    public VoteGame(List<Long> candidates, RedisPubSubGameMessagePublisher redisPubSubGameMessagePublisher, String id) {
        this.id = id;
        this.redisPubSubGameMessagePublisher = redisPubSubGameMessagePublisher;
        voteCount = new HashMap<>();
        for (Long candidate : candidates) {
            voteCount.put(candidate, new ArrayList<>());
        }
    }

    @Override
    public void publishCurrentStatus() {
        Map<String, Long> result = new HashMap<>();

        for (Map.Entry<Long, List<Long>> entry : voteCount.entrySet()) {
            result.put(entry.getKey().toString(), (long) entry.getValue().size());
        }

        redisPubSubGameMessagePublisher.publishVoteGameStatus(id, result);


        System.out.println("현재 투표 현황: " + result);
    }

    @Override
    public void clickButton(Long userId, String button) {
        Long candidateId = Long.parseLong(button);

        for (Map.Entry<Long, List<Long>> entry : voteCount.entrySet()) {
            entry.getValue().removeIf(voter -> voter.equals(userId));
        }


        if (voteCount.containsKey(candidateId)) {
            voteCount.get(candidateId).add(userId);
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
