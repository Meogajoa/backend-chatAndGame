package meogajoa.chatAndGame.domain.game.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import meogajoa.chatAndGame.domain.game.model.TeamColor;

@Getter
@Setter
@AllArgsConstructor
public class Player {
    private Long number;
    private String nickname;
    private TeamColor teamColor;
    private Long money;

    @JsonProperty("spy")
    private Boolean isSpy;

    @JsonProperty("eliminated")
    private Boolean isEliminated;

    public boolean isEliminated() {
        return isEliminated;
    }

    public void eliminate() {
        isEliminated = true;
    }
}
