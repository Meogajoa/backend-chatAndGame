package meogajoa.chatAndGame.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class RoomRequest {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class JoinRoomRequest {
        private String id;

        @Builder.Default
        private String password = "";
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class CreateRoomRequest {
        private String name;

        @Builder.Default
        private String password = "";
    }
}
