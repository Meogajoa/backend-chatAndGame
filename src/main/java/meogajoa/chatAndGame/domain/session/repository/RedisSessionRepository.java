package meogajoa.chatAndGame.domain.session.repository;

import meogajoa.chatAndGame.domain.session.entity.UserSession;
import org.springframework.data.repository.CrudRepository;

import java.util.Optional;

public interface RedisSessionRepository extends CrudRepository<UserSession, String> {

    Optional<UserSession> findByNickname(String nickname);
}
