package com.fintechguardian.externalintegrations.client;

import com.fintechguardian.externalintegrations.dto.PepCheckRequest;
import com.fintechguardian.externalintegrations.dto.PepCheckResponse;
import com.fintechguardian.externalintegrations.dto.PepProfileDto;
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
 * Cliente para APIs de PEP Screening e verificação de Pessoas Expostas Politicamente
 * Integra com serviços como ComplyAdvantage, LexisNexis, Dow Jones Risk Center
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PepScreeningClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external-integrations.pep.api-url:https://api.complyadvantage.com}")
    private String pepApiUrl;

    @Value("${external-integrations.pep.api-key:}")
    private String apiKey;

    @Value("${external-integrations.pep.timeout:30000}")
    private int timeoutMs;

    private WebClient webClient;

    /**
     * Verifica se pessoa é PEP (Pessoa Exposta Politicamente)
     */
    public Mono<PepCheckResponse> checkPepStatus(PepCheckRequest request) {
        log.debug("Checking PEP status for: {} {}", request.getFirstName(), request.getLastName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/persons/search")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Version", "v3")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(PepCheckResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .doOnSuccess(response -> {
                    if (response.isPeppy()) {
                        log.warn("PEP MATCH FOUND: {} - Risk Level: {}", 
                                request.getFirstName() + " " + request.getLastName(), 
                                response.getRiskLevel());
                    } else {
                        log.debug("PEP check completed - No matches found for: {}", 
                                request.getFirstName() + " " + request.getLastName());
                    }
                })
                .doOnError(error -> log.error("PEP check failed for: {} - {}", 
                        request.getFirstName() + " " + request.getLastName(), error.getMessage()))
                .onErrorReturn(buildErrorResponse(request));
    }

    /**
     * Busca informações detalhadas sobre pessoa específica
     */
    @Cacheable(value = "pep-profiles", key = "#personId")
    public Mono<PepProfileDto> getPepProfile(String personId) {
        log.debug("Fetching PEP profile: {}", personId);

        WebClient client = getWebClient();

        return client
                .get()
                .uri("/persons/{personId}/profile", personId)
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToMono(PepProfileDto.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("PEP profile retrieved: {}", personId))
                .doOnError(error -> log.error("Failed to retrieve PEP profile: {} - {}", 
                        personId, error.getMessage()));
    }

    /**
     * Verificação em lote para múltiplas pessoas
     */
    public Mono<java.util.List<PepCheckResponse>> batchPepCheck(java.util.List<PepCheckRequest> requests) {
        log.debug("Starting batch PEP check for {} persons", requests.size());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/persons/batch-search")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Version", "v3")
                .bodyValue(BatchPepRequest.builder()
                        .persons(requests)
                        .searchOptions(BatchPepSearchOptions.builder()
                                .includeRelatives(true)
                                .includeBusinessAssociates(true)
                                .includeMedia(false)
                                .maxResultsPerPerson(10)
                                .build())
                        .build())
                .retrieve()
                .bodyToFlux(PepCheckResponse.class)
                .collectList()
                .timeout(Duration.ofMinutes(5))
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(2))
                        .filter(this::isRetryableException))
                .doOnSuccess(responses -> log.info("Batch PEP check completed: {} processed", 
                        responses.size()))
                .doOnError(error -> log.error("Batch PEP check failed: {}", error.getMessage()));
    }

    /**
     * Verifica relacionamentos PEP (parentes, associados)
     */
    public Mono<java.util.List<PepProfileDto>> checkPepRelationships(PepCheckRequest basePerson) {
        log.debug("Checking PEP relationships for: {}", basePerson.getLastName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/persons/relationships/search")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(PepRelationshipsRequest.builder()
                        .basePerson(basePerson)
                        .relationshipTypes(java.util.List.of(
                                "spouse", "parent", "child", "sibling", 
                                "business_associate", "advisor"
                        ))
                        .maxDegreesOfSeparation(2)
                        .build())
                .retrieve()
                .bodyToFlux(PepProfileDto.class)
                .collectList()
                .timeout(Duration.ofMinutes(2))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(relationships -> log.debug("Found {} PEP relationships", relationships.size()));
    }

    /**
     * Verifica media coverage e reputação
     */
    public Mono<java.util.List<MediaProfileDto>> checkMediaProfile(PepCheckRequest person) {
        log.debug("Checking media profile for: {}", person.getLastName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/media/search")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(MediaSearchRequest.builder()
                        .person(person)
                        .dateRangeMonths(24)
                        .mediaTypes(java.util.List.of("news", "official", "social"))
                        .includeSentimentAnalysis(true)
                        .build())
                .retrieve()
                .bodyToFlux(MediaProfileDto.class)
                .collectList()
                .timeout(Duration.ofMinutes(1))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(mediaItems -> log.debug("Found {} media items", mediaItems.size()));
    }

    /**
     * Verificação de sanções internacionais específicas
     */
    public Mono<PepCheckResponse> checkInternationalSanctions(PepCheckRequest request) {
        log.debug("Checking international sanctions for: {}", request.getLastName());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/sanctions/international")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(SanctionsCheckRequest.builder()
                        .person(request)
                        .sanctionsLists(java.util.List.of(
                                "US_OFAC", "UN_UNSCR", "EU_CONSOLIDATED", 
                                "UK_HMT", "AU_DFAT"
                        ))
                        .checkAliases(true)
                        .checkVariations(true)
                        .build())
                .retrieve()
                .bodyToMono(PepCheckResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .doOnSuccess(response -> {
                    if (response.isPeppy()) {
                        log.warn("INTERNATIONAL SANCTIONS MATCH: {} - Lists: {}", 
                                request.getLastName(), response.getMatchedLists());
                    }
                });
    }

    /**
     * Monitoramento contínuo para mudanças em PEPs conhecidos
     */
    public Mono<java.util.List<PepChangeDto>> getPepUpdates(java.time.LocalDateTime lastUpdate) {
        log.debug("Fetching PEP updates since: {}", lastUpdate);

        WebClient client = getWebClient();

        return client
                .get()
                .uri("/persons/monitoring/updates?since={timestamp}", lastUpdate.toString())
                .header("Authorization", "Bearer " + apiKey)
                .retrieve()
                .bodyToFlux(PepChangeDto.class)
                .collectList()
                .timeout(Duration.ofMinutes(2))
                .doOnSuccess(updates -> log.info("Retrieved {} PEP updates", updates.size()));
    }

    // Métodos auxiliares privados

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(pepApiUrl)
                    .defaultHeader("Content-Type", "application/json")
                    .defaultHeader("Accept", "application/json")
                    .defaultHeader("User-Agent", "FinTechGuardian-PEPClient/1.0")
                    .build();
        }
        return webClient;
    }

    private boolean isRetryableException(Throwable error) {
        if (error instanceof WebClientResponseException webClientException) {
            int statusCode = webClientException.getStatusCode().value();
            return statusCode >= 500 || statusCode == 429;
        }
        return true;
    }

    private PepCheckResponse buildErrorResponse(PepCheckRequest request) {
        return PepCheckResponse.builder()
                .status("ERROR")
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .isPeppy(false)
                .riskLevel("UNKNOWN")
                .errorMessage("Failed to check PEP status due to external service error")
                .checkedAt(java.time.LocalDateTime.now())
                .build();
    }

    // Classes interno para requests específicos

    @lombok.Data
    @lombok.Builder
    public static class BatchPepRequest {
        private java.util.List<PepCheckRequest> persons;
        private BatchPepSearchOptions searchOptions;
    }

    @lombok.Data
    @lombok.Builder
    public static class BatchPepSearchOptions {
        private boolean includeRelatives;
        private boolean includeBusinessAssociates;
        private boolean includeMedia;
        private int maxResultsPerPerson;
        private String language;
    }

    @lombok.Data
    @lombok.Builder
    public static class PepRelationshipsRequest {
        private PepCheckRequest basePerson;
        private java.util.List<String> relationshipTypes;
        private int maxDegreesOfSeparation;
    }

    @lombok.Data
    @lombok.Builder
    public static class MediaSearchRequest {
        private PepCheckRequest person;
        private int dateRangeMonths;
        private java.util.List<String> mediaTypes;
        private boolean includeSentimentAnalysis;
    }

    @lombok.Data
    @lombok.Builder
    public static class SanctionsCheckRequest {
        private PepCheckRequest person;
        private java.util.List<String> sanctionsLists;
        private boolean checkAliases;
        private boolean checkVariations;
    }

    // DTOs adicionais necessários
    @lombok.Data
    @lombok.Builder
    public static class MediaProfileDto {
        private String source;
        private String headline;
        private String content;
        private String sentiment;
        private java.time.LocalDateTime publishedDate;
        private String url;
        private boolean isOfficial;
    }

    @lombok.Data
    @lombok.Builder
    public static class PepChangeDto {
        private String personId;
        private String personName;
        private String changeType;
        private String previousPosition;
        private String newPosition;
        private String changeDescription;
        private java.time.LocalDateTime changeDate;
        private String source;
    }

    @lombok.Data
    @lombok.Builder
    public static class PepProfileDto {
        private String id;
        private String fullName;
        private String position;
        private String organization;
        private String country;
        private String riskLevel;
        private java.util.List<String> aliases;
        private java.util.List<String> relationships;
        private java.time.LocalDateTime lastUpdated;
        private String source;
        private boolean isActive;
        private java.util.Map<String, Object> additionalData;
    }
}
