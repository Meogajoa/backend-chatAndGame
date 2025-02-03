package meogajoa.chatAndGame.system;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Configuration
@EnableAsync
public class ThreadPoolConfig {

    @Bean(name = "gameLogicExecutor")
    public ThreadPoolTaskExecutor gameLogicThreadPool(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("GameLogic-");
        executor.initialize();
        return executor;
    }

    @Bean(name = "gameRunningExecutor")
    public ThreadPoolTaskExecutor gameRunningThreadPool(){
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20);
        executor.setMaxPoolSize(100);
        executor.setQueueCapacity(200);
        executor.setThreadNamePrefix("gameRunning-");
        executor.initialize();
        return executor;
    }

    @Bean
    public Executor fixedThreadPoolExecutor() {
        return Executors.newFixedThreadPool(1, new ThreadFactory(){
            private final AtomicInteger threadNumber = new AtomicInteger(1);

            @Override
            public Thread newThread(Runnable r){
                return new Thread(r, "fixed-redis-listener-" + threadNumber.getAndIncrement());
            }
        });
    }
}
