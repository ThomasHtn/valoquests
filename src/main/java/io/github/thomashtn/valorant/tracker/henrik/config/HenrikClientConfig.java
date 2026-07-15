package io.github.thomashtn.valorant.tracker.henrik.config;

import io.netty.channel.ChannelOption;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configures the HTTP client used to communicate with the Henrik API.
 */
@Configuration
public class HenrikClientConfig {

    /**
     * Creates the dedicated Henrik API {@link WebClient} instance.
     *
     * @param properties Henrik API connection properties
     * @return a configured HTTP client
     */
    @Bean
    WebClient henrikWebClient(HenrikApiProperties properties) {
        HttpClient httpClient = HttpClient.create()
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Math.toIntExact(properties.connectTimeout().toMillis())
            )
            .responseTimeout(properties.readTimeout());

        WebClient.Builder builder = WebClient.builder()
            .baseUrl(properties.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient));

        if (properties.key() != null && !properties.key().isBlank()) {
            builder.defaultHeader(HttpHeaders.AUTHORIZATION, properties.key());
        }

        return builder.build();
    }
}
