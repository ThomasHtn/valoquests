package io.github.thomashtn.valoquests.shared.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * Enables Spring's asynchronous execution infrastructure and declares the executor administrative
 * commands are dispatched to.
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * Bean name referenced by {@code @Async} administrative operations.
     */
    public static final String ADMIN_TASK_EXECUTOR = "adminTaskExecutor";

    /**
     * Creates the executor running administrative commands in the background.
     *
     * <p>Single thread, queue of one; {@code MatchHistoryLock}, not this executor, keeps runs apart.
     * Overflow raises {@code TaskRejectedException}, answered as a 409.
     *
     * @return the administrative task executor
     */
    @Bean(name = ADMIN_TASK_EXECUTOR)
    public TaskExecutor adminTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(1);
        executor.setMaxPoolSize(1);
        executor.setQueueCapacity(1);
        executor.setThreadNamePrefix("admin-task-");
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();

        return executor;
    }
}
