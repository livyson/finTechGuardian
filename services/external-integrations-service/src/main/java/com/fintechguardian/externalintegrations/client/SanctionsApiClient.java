package com.fintechguardian.externalintegrations.client;

import com.fintechguardian.externalintegrations.dto.SanctionsCheckRequest;
import com.fintechguardian.externalintegrations.dto.SanctionsCheckResponse;
import com.fintechguardian.externalintegrations.dto.SanctionsProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Cliente para APIs de Sanções baseado em serviços similares aos utilizados no mercado
 * Integra com APIs como WorldCheck, Dow Jones Risk, LexisNexis, etc.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SanctionsApiClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external-integrations.sanctions.api-url:https://api.worldcheck.com}")
    private String sanctionsApiUrl;

    @Value("${external-integrations.sanctions.api-key:}")
    private String apiKey;

    @Value("${external-integrations.sanctions.timeout:30000}")
    private int timeoutMs;

    private WebClient webClient;

    /**
     * Verifica entidade contra múltiplas bases de sanções
     */
    public Mono<SanctionsCheckResponse> checkAgainstSanctions(SanctionsCheckRequest request) {
        log.debug("Checking entity against sanctions: {}", request.getEntityName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/sanctions/search")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Version", "v3")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SanctionsCheckResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .doOnSuccess(response -> log.debug("Sanctions check completed for: {}", request.getEntityName()))
                .doOnError(error -> log.error("Sanctions check failed for: {} - {}", 
                        request.getEntityName(), error.getMessage()))
                .onErrorReturn(buildErrorResponse(request));
    }

    /**
     * Busca perfis específicos por ID
     */
    @Cacheable(value = "sanctions-profiles", key = "#profileId")
    public Mono<SanctionsProfileDto> getSanctionsProfile(String profileId) {
        log.debug("Fetching sanctions profile: {}", profileId);

        WebClient client = getWebClient();

        return client
                .get()
                .uri("/sanctions/profiles/{profileId}", profileId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(SanctionsProfileDto.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Sanctions profile retrieved: {}", profileId))
                .doOnError(error -> log.error("Failed to retrieve sanctions profile: {} - {}", 
                        profileId, error.getMessage()));
    }

    /**
     * Verificação em lote para múltiplas entidades
     */
    public Mono<SanctionsCheckResponse> batchSanctionsCheck(java.util.List<SanctionsCheckRequest> requests) {
        log.debug("Starting batch sanctions check for {} entities", requests.size());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/sanctions/batch-check")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Version", "v3")
                .bodyValue(BatchSanctionsRequest.builder()
                        .requests(requests)
                        .searchOptions(BatchSearchOptions.builder()
                                .maxResultsPerRequest(50)
                                .includeAliases(true)
                                .includeMetadata(true)
                                .build())
                        .build())
                .retrieve()
                .bodyToMono(SanctionsCheckResponse.class)
                .timeout(Duration.ofMinutes(5)) // Timeout maior para batch
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableException))
                .doOnSuccess(response -> log.info("Batch sanctions check completed: {} processed", 
                        requests.size()))
                .doOnError(error -> log.error("Batch sanctions check failed: {}", error.getMessage()));
    }

    /**
     * Verifica atualizações e delta de sanções
     */
    public Mono<java.util.List<SanctionsProfileDto>> getSanctionsUpdates(String lastUpdateToken) {
        log.debug("Fetching sanctions updates since: {}", lastUpdateToken);

        WebClient client = getWebClient();

        return client
                .get()
                .uri("/sanctions/updates?since={token}", lastUpdateToken)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToFlux(SanctionsProfileDto.class)
                .collectList()
                .timeout(Duration.ofMinutes(2))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(updates -> log.info("Retrieved {} sanctions updates", updates.size()))
                .doOnError(error -> log.error("Failed to fetch sanctions updates: {}", error.getMessage()));
    }

    /**
     * Verifica entidade contra base específica (OFAC, UN, EU, etc.)
     */
    public Mono<SanctionsCheckResponse> checkAgainstSpecificSanctions(
            SanctionsCheckRequest request, SanctionsSource source) {
        
        log.debug("Checking entity against {}: {}", source.name(), request.getEntityName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/sanctions/search/{source}", source.name().toLowerCase())
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(SanctionsCheckResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)));
    }

    /**
     * Verificação de fuzzy matching para nomes similares
     */
    public Mono<SanctionsCheckResponse> fuzzySanctionsCheck(SanctionsCheckRequest request, double threshold) {
        log.debug("Performing fuzzy sanctions check with threshold {}: {}", threshold, request.getEntityName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/sanctions/fuzzy-search")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(FuzzySanctionsRequest.builder()
                        .searchData(request)
                        .threshold(threshold)
                        .includeSoundex(true)
                        .includePhonetic(true)
                        .distance("levenshtein")
                        .build())
                .retrieve()
                .bodyToMono(SanctionsCheckResponse.class)
                .timeout(Duration.ofMillis(timeoutMs * 2)); // Fuzzy matching pode ser mais lento
    }

    // Métodos auxiliares privados

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(sanctionsApiUrl)
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Accept", "application/json")
                    .defaultHeader("User-Agent", "FinTechGuardian-ExternalIntegrations/1.0")
                    .build();
        }
        return webClient;
    }

    private boolean isRetryableException(Throwable error) {
        if (error instanceof WebClientResponseException webClientException) {
            int statusCode = webClientException.getStatusCode().value();
            // 5xx server errors são retryable, 4xx geralmente não
            return statusCode >= 500 || statusCode == 429; // 429 Too Many Requests
        }
        return true; // Network timeouts etc.
    }

    private SanctionsCheckResponse buildErrorResponse(SanctionsCheckRequest request) {
        return SanctionsCheckResponse.builder()
                .entityName(request.getEntityName())
                .entityType(request.getEntityType())
                .status("ERROR")
                .matchesFound(false)
                .errorMessage("Failed to check sanctions due to external service error")
                .checkedAt(java.time.LocalDateTime.now())
                .build();
    }

    // Classes internas para requests específicos

    @lombok.Data
    @lombok.Builder
    public static class BatchSanctionsRequest {
        private java.util.List<SanctionsCheckRequest> requests;
        private BatchSearchOptions searchOptions;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchSearchOptions {
        private int maxResultsPerRequest;
        private boolean includeAliases;
        private boolean includeMetadata;
        private boolean includeDormant;
        private String language;
    }

    @lombok.Data
    @lombok.Builder
    public static class FuzzySanctionsRequest {
        private SanctionsCheckRequest searchData;
        private double threshold;
        private boolean includeSoundex;
        private boolean includePhonetic;
        private String distance;
        private int maxResults;
    }

    /**
     * Enum para fontes específicas de sanções
     */
    public enum SanctionsSource {
        OFAC("United States Treasury OFAC"),
        UN_SANCT ("United Nations Sanctions"),
        EU_SANCT("European Union Sanctions"),
        UK_HMT("UK HM Treasury"),
        AU_DFAT("Australia DFAT"),
        CA_SIR("Canada SIR"),
        WORLD_CHECK("Thompson Reuters World-Check"),
        DOW_JONES("Dow Jones Risk Center"),
        LEXIS_NEXIS("LexisNexis Bridger"),
        DIANE("Diane SanctionList"),
        CUSTOM("Custom Sanctions List");

        private final String description;

        SanctionsSource(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}
