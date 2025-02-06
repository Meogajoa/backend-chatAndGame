package meogajoa.chatAndGame.domain.room.entity;

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import java.io.Serializable;
import java.util.List;

@RedisHash("room")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class Room implements Serializable {

    @Id
    private String id;

    @Indexed
    private String name;

    private String password;

    private String owner;

    private int maxUser;

    private int currentUser;

    private boolean isLocked;

    private boolean isPlaying;

    List<String> userSessions;

}
