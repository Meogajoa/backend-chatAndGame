package meogajoa.chatAndGame.domain.game.entity;

public interface MiniGame {
    void publishCurrentStatus();
    void clickButton(Long number, String button);
    void setBlind(boolean isBlind);
    void startGame();
    void endGame();
    void cancelButton(Long number, Long target);
}
