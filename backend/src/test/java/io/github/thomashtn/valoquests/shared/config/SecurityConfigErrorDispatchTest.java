package io.github.thomashtn.valoquests.shared.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Verifies, on a real servlet container, that errors raised outside Spring MVC keep their status.
 *
 * <p>MockMvc never performs the container's ERROR dispatch to {@code /error}, so only a running
 * server shows whether the security chain lets that dispatch through.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SecurityConfigErrorDispatchTest {

    /**
     * Port of the embedded server.
     */
    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Answers a request rejected by the firewall with its 400, not an empty 403")
    void shouldKeepTheStatusOfAContainerLevelError() throws IOException, InterruptedException {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/players;x=1"))
                    .GET()
                    .build(),
                HttpResponse.BodyHandlers.ofString()
            );
        }

        assertThat(response.statusCode()).isEqualTo(400);
    }
}
