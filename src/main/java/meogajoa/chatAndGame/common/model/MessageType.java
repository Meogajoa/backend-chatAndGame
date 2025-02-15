package meogajoa.chatAndGame.common.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;


public enum MessageType {
    CHAT("CHAT"),
    SYSTEM("SYSTEM"),
    GAME_START("GAME_START"),
    TEST("TEST"),
    GAME_DAY_OR_NIGHT("GAME_DAY_OR_NIGHT"),
    MINI_GAME_WILL_START_NOTICE("MINI_GAME_WILL_START_NOTICE"),
    GAME_END("GAME_END"),
    BUTTON_GAME_STATUS("BUTTON_GAME_STATUS"),
    MINI_GAME_WILL_END_NOTICE("MINI_GAME_WILL_END_NOTICE"),
    BUTTON_CLICK("BUTTON_CLICK"),
    GAME_USER_LIST("GAME_USER_LIST"),
    GET_GAME_CHAT("GET_GAME_CHAT"),
    CHAT_LOGS("CHAT_LOGS"),
    PERSONAL_CHAT_LOGS("PERSONAL_CHAT_LOGS"),
    VOTE_GAME_STATUS("VOTE_GAME_STATUS");


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
