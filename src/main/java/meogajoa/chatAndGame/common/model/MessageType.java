package meogajoa.chatAndGame.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum MessageType {
    CHAT("CHAT"),
    SYSTEM("SYSTEM"),
    GAME_START("GAME_START"),
    TEST("TEST"),
    GAME_DAY_OR_NIGHT("GAME_DAY_OR_NIGHT"),
    GAME_END("GAME_END");

    private final String value;

    MessageType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    @JsonCreator
    public static MessageType fromValue(String value) {
        for (MessageType type : MessageType.values()) {
            if (type.value.equalsIgnoreCase(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown MessageType: " + value);
    }
}
