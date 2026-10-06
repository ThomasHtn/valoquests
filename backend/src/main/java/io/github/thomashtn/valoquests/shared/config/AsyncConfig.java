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
     * <p>Single-threaded with a queue of one. This executor does not keep runs apart on its own:
     * scheduled jobs run on the scheduler thread, not here. {@code MatchHistoryLock} does, and
     * refuses a concurrent request with a 409 before it reaches this executor.
     *
     * <p>The queue only absorbs a run accepted while the previous task, which has already released
     * the lock, is still leaving its thread. Anything beyond is rejected with a
     * {@code TaskRejectedException}, answered as a 409 as well.
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
