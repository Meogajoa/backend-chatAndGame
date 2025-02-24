package meogajoa.chatAndGame.domain.game.model;

public enum MiniGameType {
    BUTTON_CLICK("BUTTON_CLICK"),
    VOTE_GAME("VOTE_GAME"),
    RE_VOTE_GAME("RE_VOTE_GAME"),;

    private final String name;

    MiniGameType(String name) {
        this.name = name;
    }
}
