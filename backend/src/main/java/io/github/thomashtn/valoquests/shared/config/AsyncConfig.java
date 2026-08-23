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
     * <p>Deliberately single-threaded with a queue of one. A synchronization walks the Henrik match
     * history under a shared rate-limit budget of a few dozen requests per minute, so running two
     * at once would not make either finish sooner — it would only make both wait longer, and would
     * let two walks import the same matches concurrently. Serializing them keeps the budget spent
     * on one run at a time.
     *
     * <p>The queue exists so a request accepted just as the previous run finishes is not rejected
     * by the pool itself; concurrent <em>requests</em> are refused earlier and explicitly, with a
     * 409, rather than being silently queued behind a run the caller cannot see.
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
