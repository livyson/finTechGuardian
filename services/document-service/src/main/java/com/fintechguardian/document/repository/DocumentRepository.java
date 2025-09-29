package com.fintechguardian.document.repository;

import com.fintechguardian.document.entity.Document;
import java.util.Map;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository MongoDB para operações CRUD de documentos
 * Inclui queries customizadas para casos específicos de compliance
 */
@Repository
public interface DocumentRepository extends MongoRepository<Document, String>, CustomDocumentRepositoryInterface {

    /**
     * Busca documentos por cliente
     */
    List<Document> findByCustomerIdOrderByCreatedAtDesc(String customerId);

    /**
     * Busca documentos por tipo especifico
     */
    List<Document> findByCustomerIdAndDocumentTypeOrderByCreatedAtDesc(
            String customerId, Document.DocumentType documentType);

    /**
     * Busca documentos por categoria de compliance
     */
    List<Document> findByCategoryAndStatusOrderByCreatedAtDesc(
            Document.DocumentCategory category, Document.DocumentStatus status);

    /**
     * Busca documentos associados a uma entidade (caso, transação, etc.)
     */
    List<Document> findByEntityTypeAndEntityIdOrderByCreatedAtDesc(
            String entityType, String entityId);

    /**
     * Busca documentos por status e período
     */
                                List<Document> findByStatusAndCreatedAtBetweenOrderByCreatedAtDesc(
            Document.DocumentStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Busca documentos com legal hold ativa
     */
    @Query("{'hasLegalHold': true, $or: [{'legalHoldUntil': {$gt: ?0}}, {'legalHoldUntil': null}]}")
    List<Document> findDocumentsWithActiveLegalHold(LocalDateTime currentDate);

    /**
     * Busca documentos que precisam ser processados para OCR
     */
    @Query("{'status': 'PROCESSING', 'extractedText': {$exists: false}, 'documentType': {$in: ['IDENTIFICATION', 'DRIVERS_LICENSE', 'PASSPORT', 'OTHER']}}")
    List<Document> findDocumentsPendingOcrProcessing();

    /**
     * Busca documentos com alto risco de fraude
     */
    @Query("{'fraudRiskScore': {$gte: 0.8}}")
    List<Document> findDocumentsWithHighFraudRisk();

    /**
     * Busca documentos expirando em breve
     */
    @Query("{'expiresAt': {$gte: ?0, $lt: ?1}}")
    List<Document> findDocumentsExpiringSoon(LocalDateTime from, LocalDateTime to);

    /**
     * Busca documentos para arquivamento baseado em políticas
     */
    @Query("{'retentionExpiresAt': {$lt: ?0}, 'status': {$nin: ['ARCHIVED', 'DELETED']}}")
    List<Document> findDocumentsReadyForArchive(LocalDateTime currentDate);

    /**
     * Busca documentos que expiraram há mais de X dias
     */
    @Query("{'expiresAt': {$lt: ?0}, 'status': {$nin: ['ARCHIVED', 'DELETED']}}")
    List<Document> findExpiredDocuments(LocalDateTime expiredBefore);

    /**
     * Busca documentos por múltiplas tags
     */
    @Query("{'tags': {$in: ?0}}")
    List<Document> findByTagsContainingAny(List<String> tags);

    /**
     * Busca documentos por atributo customizado
     */
    @Query("{'customAttributes.?0': ?1}")
    List<Document> findByCustomAttribute(String attributeKey, String attributeValue);

    /**
     * Busca documentos com metadata específica
     */
    @Query("{'extractedMetadata.?0': ?1}")
    List<Document> findByExtractedMetadata(String metadataKey, Object metadataValue);

    /**
     * Busca documentos compartilhados com usuário específico
     */
    @Query("{'sharedWithUsers.?0': {$exists: true}}")
    List<Document> findDocumentsSharedWithUser(String userId);

    /**
     * Busca documentos por versão (histórico)
     */
    List<Document> findByParentDocumentIdOrderByVersionNumberDesc(String parentDocumentId);

    /**
     * Busca documentos por tipo de fraude detectada
     */
    @Query("{'fraudIndicators.?0': {$exists: true, $ne: null}}")
    List<Document> findByFraudIndicatorType(String fraudType);

    /**
     * Busca documentos do mesmo cliente com mesmo hash (duplicados)
     */
    @Query("{'customerId': ?0, 'contentHashForDeduplication': ?1}")
    Optional<Document> findDuplicateByCustomerAndHash(String customerId, String contentHash);

    /**
     * Busca documentos por nível de confiança do OCR
     */
    @Query("{'ocrConfidence.average': {$gte: ?0}}")
    List<Document> findDocumentsWithHighOcrConfidence(Double minConfidence);

    /**
     * Busca documentos por categorias de compliance específicas
     */
    @Query("{'category': {$in: ['AML_EVIDENCE', 'COMPLIANCE_DOCUMENT'], 'status': 'READY'}")
    List<Document> findComplianceDocumentsReady();

    /**
     * Busca documentos compartilhables (não protegidos)
     */
    @Query("{'isPublic': true, 'shareToken': {$exists: true}, 'shareExpiresAt': {$gt: ?0}}")
    List<Document> findPublicShareableDocuments(LocalDateTime currentDate);

    /**
     * Busca documentos grandes (>10MB) para otimização
     */
    @Query("{'fileSize': {$gt: 10485760}}")
    List<Document> findLargeDocuments();

    /**
     * Busca documentos com processamento em erro
     */
    @Query("{'status': 'ERROR', 'processingStatus': {$exists: true}}")
    List<Document> findDocumentsWithProcessingErrors();

    /**
     * Busca documentos recentes que ainda não foram classificados
     */
    @Query("{'classification': {$exists: false}, 'createdAt': {$gte: ?0}}")
    List<Document> findRecentUnclassifedDocuments(LocalDateTime since);

    /**
     * Conta documentos por cliente e status
     */
    @Query(value = "{'customerId': ?0, 'status': ?1}", count = true)
    Long countDocumentsByCustomerAndStatus(String customerId, Document.DocumentStatus status);

    /**
     * Busca documentos para auditoria - última modificação
     */
    @Query("{'updatedAt': {$gte: ?0}, 'customerId': ?1}")
    List<Document> findRecentlyUpdatedDocuments(LocalDateTime since, String customerId);

    /**
     * Busca documentos com assinatura detectada
     */
    @Query("{'hasSignatures': true, 'extractedMetadata.signatureCount': {$gte: 1}}")
    List<Document> findDocumentsWithSignatures();

    /**
     * Busca documentos por origem específica (upload, email, scanner)
     */
    List<Document> findBySourceOrderByCreatedAtDesc(String source);

    /**
     * Busca documentos que precisam de revisão manual
     */
    @Query("{'status': 'UNDER_REVIEW', 'fraudRiskScore': {$gte: 0.5}}")
    List<Document> findDocumentsRequiringManualReview();
}

/**
 * Interface para operações customizadas de documento
 * Implementado para casos complexos que requerem agregações MongoDB
 */
interface CustomDocumentRepositoryInterface {
    
    /**
     * Busca documentos com agregação complexa para dashboard
     */
    DocumentAggregationResult findDocumentMetrics(String customerId, LocalDateTime from, LocalDateTime to);
    
    /**
     * Busca documentos por similaridade textual (para detecção de duplicatas)
     */
    List<DocumentSimilarity> findSimilarDocuments(String documentId, double threshold);
    
    /**
     * Atualiza status de múltiplos documentos em batch
     */
    long updateDocumentStatusBatch(List<String> documentIds, Document.DocumentStatus newStatus);
    
    /**
     * Busca documentos por proximidade geográfica (se aplicável)
     */
    List<Document> findDocumentsByGeographicProximity(double latitude, double longitude, double radiusKm);
}

/**
 * Resultados de agregação para métricas de documento
 */
record DocumentAggregationResult(
    long totalDocuments,
    long documentsByStatus,
    long documentsByType,
    long documentsByCategory,
    double averageFileSize,
    double averageProcessingTimeHours,
    Map<String, Long> documentsBySource,
    List<String> topDocumentTypes,
    List<String> topTags
) {}

/**
 * Similaridade entre documentos
 */
record DocumentSimilarity(
    String documentId,
    String similarDocumentId,
    double similarity,
    String similarityType // TEXT, VISUAL, METADATA
) {}
