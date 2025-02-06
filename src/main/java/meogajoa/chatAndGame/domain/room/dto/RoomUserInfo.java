package meogajoa.chatAndGame.domain.room.dto;

import lombok.*;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Getter
@Setter
@Data
public class RoomUserInfo {
    private String roomId;
    private List<String> users;

    public static RoomUserInfo from(List<String> users, String roomId){
        return RoomUserInfo.builder()
                .roomId(roomId)
                .users(users)
                .build();
    }


}
