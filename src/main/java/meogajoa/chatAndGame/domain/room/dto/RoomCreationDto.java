package meogajoa.chatAndGame.domain.room.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


public class RoomCreationDto {

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RoomCreationRequest {
        private String roomName;
        private String roomPassword;
        private int roomMaxUser;
    }

    @AllArgsConstructor
    @NoArgsConstructor
    @Data
    @Builder
    public static class RoomCreationResponse {
        private String id;
    }
}
