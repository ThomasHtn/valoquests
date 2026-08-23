package io.github.thomashtn.valoquests.henrik.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import java.util.concurrent.TimeUnit;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * Configures the HTTP infrastructure dedicated to the HenrikDev API.
 */
@Configuration
public class HenrikClientConfig {

    /**
     * User agent sent with every Henrik API request.
     */
    private static final String USER_AGENT = "valo-quests/1.0";

    /**
     * Maximum Henrik response size buffered in memory.
     *
     * <p>Henrik v4 match-history responses contain detailed round, player and
     * damage data. A page of ten matches can exceed four megabytes, especially
     * for long competitive matches. A bounded sixteen-megabyte limit supports
     * these payloads while preventing unlimited buffering.</p>
     */
    private static final int MAX_RESPONSE_SIZE_BYTES =
        16 * 1024 * 1024;

    /**
     * Creates the Reactor Netty HTTP client used by the Henrik {@link WebClient}.
     *
     * @param properties validated Henrik API configuration
     * @return configured Reactor Netty HTTP client
     */
    private HttpClient createHttpClient(HenrikApiProperties properties) {
        return HttpClient.create()
            .option(
                ChannelOption.CONNECT_TIMEOUT_MILLIS,
                Math.toIntExact(properties.connectTimeout().toMillis())
            )
            .responseTimeout(properties.readTimeout())
            .doOnConnected(connection -> connection
                .addHandlerLast(
                    new ReadTimeoutHandler(
                        properties.readTimeout().toMillis(),
                        TimeUnit.MILLISECONDS
                    )
                )
                .addHandlerLast(
                    new WriteTimeoutHandler(
                        properties.readTimeout().toMillis(),
                        TimeUnit.MILLISECONDS
                    )
                )
            );
    }

    /**
     * Creates the dedicated HTTP client used for Henrik API calls.
     *
     * <p>The bean contains only technical HTTP configuration. Endpoint paths,
     * request parameters and response mappings remain in the Henrik client
     * implementation.</p>
     *
     * @param properties validated Henrik API configuration
     * @return configured Henrik API {@link WebClient}
     */
    @Bean
    public WebClient henrikWebClient(HenrikApiProperties properties) {
        HttpClient httpClient = createHttpClient(properties);

        ExchangeStrategies exchangeStrategies =
            ExchangeStrategies.builder()
                .codecs(configurer ->
                    configurer.defaultCodecs()
                        .maxInMemorySize(MAX_RESPONSE_SIZE_BYTES)
                )
                .build();

        return WebClient.builder()
            .baseUrl(properties.baseUrl())
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(exchangeStrategies)
            .defaultHeader(HttpHeaders.AUTHORIZATION, properties.key())
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
            .build();
    }
}
