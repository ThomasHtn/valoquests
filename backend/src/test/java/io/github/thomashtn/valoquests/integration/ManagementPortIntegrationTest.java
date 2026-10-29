package io.github.thomashtn.valoquests.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalManagementPort;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Specifies the probes on the main port and the actuator on its own port, open to probes and scrapers.
 *
 * <p>A real server is started: MockMvc only sees the main port.
 */
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ManagementPortIntegrationTest extends PostgreSqlIntegrationTest {

    /**
     * Client for the real servers.
     */
    private static final HttpClient CLIENT = HttpClient.newHttpClient();

    /**
     * Random main port of this test server.
     */
    @LocalServerPort
    private int serverPort;

    /**
     * Random management port of this test server.
     */
    @LocalManagementPort
    private int managementPort;

    @Test
    @DisplayName("Answers the readiness probe on the main port once the database is reachable")
    void exposesReadinessOnTheMainPort() throws Exception {
        HttpResponse<String> response = get(serverPort, "/readyz");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"status\":\"UP\"");
    }

    @Test
    @DisplayName("Answers the liveness probe on the main port without a key")
    void exposesLivenessOnTheMainPort() throws Exception {
        assertThat(get(serverPort, "/livez").statusCode()).isEqualTo(200);
    }

    @Test
    @DisplayName("Keeps the actuator off the main port, so a proxy routing it publishes nothing")
    void hidesTheActuatorFromTheMainPort() throws Exception {
        assertThat(get(serverPort, "/actuator/prometheus").statusCode()).isNotEqualTo(200);
    }

    @Test
    @DisplayName("Publishes the running build on the info endpoint, so a deployment can be checked")
    void exposesBuildInfo() throws Exception {
        HttpResponse<String> response = get(managementPort, "/actuator/info");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("\"artifact\":\"valo-quests\"");
    }

    @Test
    @DisplayName("Serves Prometheus metrics without a key, tagged with the application name")
    void exposesPrometheusMetrics() throws Exception {
        HttpResponse<String> response = get(managementPort, "/actuator/prometheus");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("hikaricp_connections_active{application=\"valo-quests\"");
    }

    /**
     * Sends a GET to one of the test servers.
     *
     * @param port main or management port
     * @param path absolute path on that port
     * @return response with its body as text
     * @throws IOException when the request fails
     * @throws InterruptedException when interrupted while waiting
     */
    private HttpResponse<String> get(int port, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build();
        return CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
