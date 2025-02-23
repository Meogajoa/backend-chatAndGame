package meogajoa.chatAndGame.domain.game.listener;

public interface MiniGameListener {
    void onGameEnd();
    String findNicknameByPlayerNumber(Long number);
    Long findPlayerNumberByNickname(String nickname);
}
