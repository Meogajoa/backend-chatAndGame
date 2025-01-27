package meogajoa.chatAndGame.domain.game.model;

import lombok.Getter;

@Getter
public enum TeamColor {
    BLACK("black"),
    WHITE("white");

    private final String value;

    TeamColor(String value) {
        this.value = value;
    }

}
