package com.fintechguardian.externalintegrations.client;

import com.fintechguardian.externalintegrations.dto.PepCheckRequest;
import com.fintechguardian.externalintegrations.dto.PepCheckResponse;
import com.fintechguardian.externalintegrations.dto.PepProfileDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Cliente para APIs de Document Verification e Validação de Documentos
 * Integra com serviços como Jumio, Onfido, Shufti Pro, Trulioo
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentVerificationClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${external-integrations.document-verification.api-url:https://api.onfido.com}")
    private String verificationApiUrl;

    @Value("${external-integrations.document-verification.api-key:}")
    private String apiKey;

    @Value("${external-integrations.document-verification.timeout:60000}")
    private int timeoutMs;

    private WebClient webClient;

    /**
     * Verifica documentos de identidade (CPF, CNH, RG, Passport)
     */
    public Mono<DocumentVerificationResponse> verifyIdentityDocument(DocumentVerificationRequest request) {
        log.debug("Verifying identity document: {}", request.getDocumentType());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/documents/verify")
                .header("Authorization", "Bearer " + apiKey)
                .header("X-API-Version", "v3")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DocumentVerificationResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1))
                        .filter(this::isRetryableException))
                .doOnSuccess(response -> {
                    if (response.isVerified()) {
                        log.debug("Document verified successfully: {}", request.getDocumentNumber());
                    } else {
                        log.warn("Document verification failed: {} - Reason: {}", 
                                request.getDocumentNumber(), response.getFailureReason());
                    }
                })
                .doOnError(error -> log.error("Document verification error: {}", error.getMessage()))
                .onErrorReturn(buildErrorResponse(request));
    }

    /**
     * Verifica documentos de empresa (CNPJ, Contratos, Cartas de Constituição)
     */
    public Mono<BusinessDocumentResponse> verifyBusinessDocument(BusinessDocumentRequest request) {
        log.debug("Verifying business document: {}", request.getDocumentType());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/business/documents/verify")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BusinessDocumentResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Business document verification completed"))
                .doOnError(error -> log.error("Business document verification failed: {}", error.getMessage()));
    }

    /**
     * OCR (Optical Character Recognition) para extrair dados de documentos
     */
    public Mono<OcrResponse> performOcrExtraction(OcrRequest request) {
        log.debug("Performing OCR extraction for document type: {}", request.getDocumentType());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/ocr/extract")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "multipart/form-data")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(OcrResponse.class)
                .timeout(Duration.ofMinutes(2)) // OCR pode demorar mais
                .retryWhen(Retry.fixedDelay(3, Duration.ofSeconds(2)))
                .doOnSuccess(response -> log.debug("OCR extraction completed - confidence: {}", 
                        response.getConfidenceScore()))
                .doOnError(error -> log.error("OCR extraction failed: {}", error.getMessage()));
    }

    /**
     * Verificação de biometria facial com documento
     */
    public Mono<BiometricVerificationResponse> verifyBiometricMatch(
            BiometricVerificationRequest request) {
        
        log.debug("Performing biometric verification for customer: {}", request.getCustomerId());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/biometric/verify-match")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BiometricVerificationResponse.class)
                .timeout(Duration.ofMinutes(3)) // Biometria pode ser mais demorada
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> {
                    if (response.isMatch()) {
                        log.debug("Biometric match confirmed for customer: {}", request.getCustomerId());
                    } else {
                        log.warn("Biometric match failure for customer: {} - Score: {}", 
                                request.getCustomerId(), response.getMatchScore());
                    }
                })
                .doOnError(error -> log.error("Biometric verification failed: {}", error.getMessage()));
    }

    /**
     * Verificação de assinatura digital
     */
    public Mono<DigitalSignatureResponse> verifyDigitalSignature(DigitalSignatureRequest request) {
        log.debug("Verifying digital signature for document: {}", request.getDocumentId());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/signature/verify")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(DigitalSignatureResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> log.debug("Digital signature verification completed"))
                .doOnError(error -> log.error("Digital signature verification failed: {}", error.getMessage()));
    }

    /**
     * Análise de documento para detectar fraudes e adulterações
     */
    public Mono<FraudAnalysisResponse> analyzeFraud(DocumentAnalysisRequest request) {
        log.debug("Performing fraud analysis for document: {}", request.getDocumentId());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/fraud/analyze")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(FraudAnalysisResponse.class)
                .timeout(Duration.ofMinutes(2))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> {
                    if (response.getFraudRisk() > 0.7) {
                        log.warn("HIGH FRAUD RISK detected for document {} - Score: {}", 
                                request.getDocumentId(), response.getFraudRisk());
                    }
                })
                .doOnError(error -> log.error("Fraud analysis failed: {}", error.getMessage()));
    }

    /**
     * Verificação de listas negras/restritivas do Brasil
     */
    public Mono<BrazilianRestrictionsResponse> checkBrazilianRestrictions(CPFVerificationRequest request) {
        log.debug("Checking Brazilian restrictions for CPF: {}", request.getCpf());

        WebClient client = getWebClient();

        return client
                .post()
                .uri("/restrictions/brazil")
                .header("Authorization", "Bearer " + apiKey)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(BrazilianRestrictionsResponse.class)
                .timeout(Duration.ofMillis(timeoutMs))
                .retryWhen(Retry.fixedDelay(2, Duration.ofSeconds(1)))
                .doOnSuccess(response -> {
                    if (response.hasRestrictions()) {
                        log.warn("BRAZILIAN RESTRICTIONS FOUND for CPF {}: {}", 
                                request.getCpf(), response.getRestrictionTypes());
                    }
                })
                .doOnError(error -> log.error("Brazilian restrictions check failed: {}", error.getMessage()));
    }

    // Métodos auxiliares privados

    private WebClient getWebClient() {
        if (webClient == null) {
            webClient = webClientBuilder
                    .baseUrl(verificationApiUrl)
                    .defaultHeader("Accept", "application/json")
                    .defaultHeader("User-Agent", "FinTechGuardian-DocumentVerification/1.0")
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

    private DocumentVerificationResponse buildErrorResponse(DocumentVerificationRequest request) {
        return DocumentVerificationResponse.builder()
                .documentType(request.getDocumentType())
                .documentNumber(request.getDocumentNumber())
                .verified(false)
                .status("ERROR")
                .failureReason("Document verification failed due to external service error")
                .verifiedAt(java.time.LocalDateTime.now())
                .build();
    }

    // Classes internas para requests/responses específicas

    @lombok.Data
    @lombok.Builder
    public static class DocumentVerificationRequest {
        private String documentType;
        private String documentNumber;
        private String documentImageFront;
        private String documentImageBack;
        private String selfieImage;
        private Map<String, Object> additionalData;
    }

    @lombok.Data
    @lombok.Builder
    public static class DocumentVerificationResponse {
        private String documentType;
        private String documentNumber;
        private boolean verified;
        private String status;
        private Double confidenceScore;
        private String failureReason;
        private Map<String, String> extractedData;
        private java.time.LocalDateTime verifiedAt;
        private String verificationId;
        private java.util.List<String> warnings;
    }

    @lombok.Data
    @lombok.Builder
    public static class BusinessDocumentRequest {
        private String documentType;
        private String documentImage;
        private String companyName;
        private String cnpj;
        private Map<String, Object> additionalData;
    }

    @lombok.Data
    @lombok.Builder
    public static class BusinessDocumentResponse {
        private boolean verified;
        private String status;
        private Double confidenceScore;
        private Map<String, String> extractedData;
        private String verificationId;
    }

    @lombok.Data
    @lombok.Builder
    public static class OcrRequest {
        private String documentImage;
        private String documentType;
        private String language;
        private Map<String, Object> extractionRules;
    }

    @lombok.Data
    @lombok.Builder
    public static class OcrResponse {
        private Double confidenceScore;
        private Map<String, String> extractedText;
        private String ocrEngineUsed;
        private java.util.List<String> warnings;
        private Map<String, Object> metadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class BiometricVerificationRequest {
        private String customerId;
        private String documentImage;
        private String livePhoto;
        private String videoSelfie;
        private Map<String, Object> biometricData;
    }

    @lombok.Data
    @lombok.Builder
    public static class BiometricVerificationResponse {
        private boolean isMatch;
        private Double matchScore;
        private String status;
        private String algorithmUsed;
        private java.time.LocalDateTime verifiedAt;
        private java.util.List<String> qualityIssues;
    }

    @lombok.Data
    @lombok.Builder
    public static class DigitalSignatureRequest {
        private String documentId;
        private String documentHash;
        private String certificate;
        private String signature;
        private Map<String, Object> signatureMetadata;
    }

    @lombok.Data
    @lombok.Builder
    public static class DigitalSignatureResponse {
        private boolean isValid;
        private String certificateAuthority;
        private java.time.LocalDateTime signedAt;
        private String signerInfo;
        private boolean certificateRevoked;
        private Map<String, String> certificateDetails;
    }

    @lombok.Data
    @lombok.Builder
    public static class DocumentAnalysisRequest {
        private String documentId;
        private String documentImage;
        private String analysisType;
        private Map<String, Object> analysisParameters;
    }

    @lombok.Data
    @lombok.Builder
    public static class FraudAnalysisResponse {
        private Double fraudRisk;
        private String riskLevel;
        private java.util.List<String> detectedCharacteristics;
        private java.util.List<String> anomalies;
        private Map<String, Double> riskFactors;
        private String recommendation;
    }

    @lombok.Data
    @lombok.Builder
    public static class CPFVerificationRequest {
        private String cpf;
        private String name;
        private java.time.LocalDate birthDate;
        private Map<String, Object> additionalData;
    }

    @lombok.Data
    @lombok.Builder
    public static class BrazilianRestrictionsResponse {
        private boolean hasRestrictions;
        private java.util.List<String> restrictionTypes;
        private String restrictionSource;
        private java.time.LocalDateTime checkedAt;
        private Map<String, Object> restrictionDetails;
    }
}
