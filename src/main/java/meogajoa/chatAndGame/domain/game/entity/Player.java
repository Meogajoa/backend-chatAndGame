package meogajoa.chatAndGame.domain.game.entity;

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
    private Boolean isSpy;
}
