package meogajoa.chatAndGame.system;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import meogajoa.chatAndGame.domain.chat.publisher.RedisPubSubChatPublisher;
import meogajoa.chatAndGame.domain.chat.repository.CustomRedisChatLogRepository;
import meogajoa.chatAndGame.domain.game.manager.GameSessionManager;
import meogajoa.chatAndGame.domain.subscriber.AsyncStreamHandler;
import meogajoa.chatAndGame.domain.subscriber.SyncStreamHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamReceiver;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@RequiredArgsConstructor
@Configuration
@Slf4j
public class RedisStreamConfig {

    private Disposable streamSubscription;
    private final AsyncStreamHandler asyncStreamHandler;
    private final SyncStreamHandler syncStreamHandler;
    private final Executor fixedThreadPoolExecutor;

    @Bean
    public Disposable streamReceiver(ReactiveRedisConnectionFactory factory){
        StreamReceiver<String, MapRecord<String, String, String>> receiver = StreamReceiver.create(factory);

        Flux<MapRecord<String, String, String>> syncStream = receiver.receive(StreamOffset.create("stream:sync:", ReadOffset.lastConsumed()));
        Flux<MapRecord<String, String, String>> asyncStream = receiver.receive(StreamOffset.create("stream:async:", ReadOffset.lastConsumed()));

        this.streamSubscription = Flux.merge(asyncStream, syncStream)
                .subscribeOn(Schedulers.fromExecutor(fixedThreadPoolExecutor))
                .publishOn(Schedulers.fromExecutor(fixedThreadPoolExecutor))
                .subscribe(record -> {
                    String streamKey = record.getStream();
                    log.info("메시지 받음 - 현재 스레드 : " + Thread.currentThread().getName());

                    if("stream:async:".equals(streamKey)){
                        asyncStreamHandler.handleMessage(record);
                    }else if("stream:sync:".equals(streamKey)){
                        syncStreamHandler.handleMessage(record);
                    }else{
                        log.warn("Unknown stream key: {}", streamKey);
                    }
                }, error -> {
                    log.error("Error occurred while processing stream", error);
                });


        return this.streamSubscription;
    }

    @PreDestroy
    public void cleanup() {
        if (this.streamSubscription != null && !this.streamSubscription.isDisposed()) {
            this.streamSubscription.dispose();
        }
    }



}
