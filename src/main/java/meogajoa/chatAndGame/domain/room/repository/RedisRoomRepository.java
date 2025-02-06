package meogajoa.chatAndGame.domain.room.repository;

import meogajoa.chatAndGame.domain.room.entity.Room;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RedisRoomRepository extends CrudRepository<Room, String> {

}
