package meogajoa.chatAndGame.domain.room.repository;

import lombok.RequiredArgsConstructor;
import meogajoa.chatAndGame.domain.room.entity.Room;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.*;

@Repository
@RequiredArgsConstructor
public class CustomRedisRoomRepository {
    private final StringRedisTemplate stringRedisTemplate;

    private static final String ROOM_KEY_PREFIX = "room:";
    private static final String AVAILABLE_ROOM_LIST_KEY = "availableRoomList:";

    private static final String USING_ROOM_LIST_KEY = "usingRoomList:";

    public void saveUserToRoom(String nickname, Room room) {
        String userKey = ROOM_KEY_PREFIX + room.getId() + ":users";
        String roomKey = ROOM_KEY_PREFIX + room.getId();
        stringRedisTemplate.opsForSet().add(userKey, nickname);
        stringRedisTemplate.opsForHash().put(roomKey, "currentUser", String.valueOf(room.getCurrentUser() + 1));
    }

    public String getRoomName(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        return (String) stringRedisTemplate.opsForHash().get(roomKey, "name");
    }

    public boolean isAlreadyExistRoom(String roomId) {
        return roomId != null && stringRedisTemplate.hasKey(ROOM_KEY_PREFIX + roomId);
    }

    public String getRoomOwner(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId;
        return (String) stringRedisTemplate.opsForHash().get(roomKey, "owner");
    }

    public boolean isUserInRoom(String nickname, String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId + ":users";
        return Boolean.TRUE.equals(stringRedisTemplate.opsForSet().isMember(roomKey, nickname));
    }

    private Room mapToRoom(Map<Object, Object> roomData) {
        Room room = new Room();
        room.setId((String) roomData.get("id"));
        room.setName((String) roomData.get("name"));
        room.setPassword((String) roomData.get("password"));
        room.setOwner((String) roomData.get("owner"));
        room.setMaxUser(Integer.parseInt((String) roomData.get("maxUser")));
        room.setCurrentUser(Integer.parseInt((String) roomData.get("currentUser")));
        room.setPlaying(Boolean.parseBoolean((String) roomData.get("isPlaying")));
        room.setLocked(Boolean.parseBoolean((String) roomData.get("isLocked")));
        return room;
    }

    public List<String> getUserNicknameInRoom(String roomNumber) {
        String roomKey = ROOM_KEY_PREFIX + roomNumber + ":users";
        return new ArrayList<>(stringRedisTemplate.opsForSet().members(roomKey));
    }

    public List<String> getUserSessionIdInRoom(String roomNumber) {
        List<String> nicknames = getUserNicknameInRoom(roomNumber);
        List<String> sessionIds = new ArrayList<>();
        for (String nickname : nicknames) {
            String sessionId = stringRedisTemplate.opsForValue().get("nicknameToSessionId:" + nickname);
            if (sessionId != null) {
                sessionIds.add(sessionId);
            }
        }
        return sessionIds;
    }

    public List<String> getUsersInRoom(String roomId) {
        String roomKey = ROOM_KEY_PREFIX + roomId + ":users";
        return new ArrayList<>(stringRedisTemplate.opsForSet().members(roomKey));
    }

    public boolean isPlaying(String id) {
        String roomKey = ROOM_KEY_PREFIX + id;
        return Boolean.parseBoolean((String) stringRedisTemplate.opsForHash().get(roomKey, "isPlaying"));
    }
}
