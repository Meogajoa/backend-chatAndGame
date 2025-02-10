package meogajoa.chatAndGame.domain.game.model;

import lombok.Getter;

@Getter
public enum TeamColor {
    BLACK("black"),
    WHITE("white"),
    RED("red");

    private final String value;

    TeamColor(String value) {
        this.value = value;
    }

}
