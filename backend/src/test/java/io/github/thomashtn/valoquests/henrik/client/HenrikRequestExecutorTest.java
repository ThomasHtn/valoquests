package io.github.thomashtn.valoquests.henrik.client;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.thomashtn.valoquests.henrik.config.HenrikApiProperties;
import io.github.thomashtn.valoquests.henrik.exception.HenrikApiException;
import io.github.thomashtn.valoquests.henrik.exception.HenrikRequestTimeoutException;
import io.netty.handler.timeout.ReadTimeoutException;
import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;

/**
 * Verifies that {@link HenrikRequestExecutor} wraps every transport-level failure into a retryable
 * exception, and specifically that both distinct {@code TimeoutException} classes it may receive
 * from {@code WebClient} are recognized as timeouts despite sharing an identical simple name.
 */
class HenrikRequestExecutorTest {

    /**
     * Verifies that a JDK timeout is wrapped as a Henrik timeout exception.
     */
    @Test
    void shouldWrapJdkTimeoutAsHenrikTimeout() {
        assertThatThrownBy(
            () -> execute(new TimeoutException("read timed out"))
        )
            .isInstanceOf(HenrikRequestTimeoutException.class);
    }

    /**
     * Verifies that a Netty timeout is wrapped as a Henrik timeout exception. This is the exact
     * regression this test guards against: {@code io.netty.handler.timeout.TimeoutException} and
     * {@code java.util.concurrent.TimeoutException} are distinct, unrelated classes, and dropping
     * either check from {@link HenrikRequestExecutor} would silently stop retrying real timeouts.
     */
    @Test
    void shouldWrapNettyTimeoutAsHenrikTimeout() {
        assertThatThrownBy(
            () -> execute(ReadTimeoutException.INSTANCE)
        )
            .isInstanceOf(HenrikRequestTimeoutException.class);
    }

    /**
     * Verifies that a non-timeout transport failure is wrapped as a generic retryable exception,
     * not misclassified as a timeout.
     */
    @Test
    void shouldWrapOtherTransportFailureAsGenericException() {
        assertThatThrownBy(
            () -> execute(new IOException("connection reset"))
        )
            .isInstanceOf(HenrikApiException.class)
            .isNotInstanceOf(HenrikRequestTimeoutException.class);
    }

    /**
     * Executes a request whose {@code WebClient} call fails at the transport level with the given
     * cause, using a single-attempt budget so the mapped exception surfaces immediately.
     *
     * @param transportCause cause wrapped by the simulated {@link WebClientRequestException}
     */
    private void execute(Throwable transportCause) {
        HenrikApiProperties properties = new HenrikApiProperties(
            "http://localhost",
            "test-api-key",
            "eu",
            "pc",
            Duration.ofSeconds(2),
            Duration.ofSeconds(2),
            1,
            Duration.ofMillis(1),
            1,
            30,
            Duration.ZERO
        );

        HenrikRequestExecutor executor = new HenrikRequestExecutor(
            new HenrikRetryStrategy(properties),
            new HenrikRequestLimiter(properties)
        );

        WebClientRequestException failure = new WebClientRequestException(
            transportCause,
            HttpMethod.GET,
            URI.create("http://localhost/"),
            new HttpHeaders()
        );

        executor.execute(
            "test-operation",
            () -> Mono.error(failure)
        );
    }
}
