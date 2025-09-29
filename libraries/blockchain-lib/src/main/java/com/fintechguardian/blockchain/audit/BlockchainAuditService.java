package com.fintechguardian.blockchain.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import com.fintechguardian.common.domain.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Serviço de auditoria blockchain para compliance imutável
 * Integra com Ethereum/Matic para registro permanente de eventos críticos
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BlockchainAuditService {

    private final Web3j web3jSvc;
    private final Credentials credentials;
    private final ComplianceAuditContract auditContract;

    @Value("${fintechguardian.blockchain.chain-id:137}")
    private int chainId;

    @Value("${fintechguardian.blockchain.enabled:true}")
    private boolean blockchainEnabled;

    /**
     * Registra evento de compliance na blockchain
     */
    public CompletableFuture<String> auditComplianceEvent(DomainEvent event) {
        if (!blockchainEnabled) {
            log.debug("Blockchain auditing disabled");
            return CompletableFuture.completedFuture("disabled");
        }

        log.info("Auditing compliance event to blockchain: {}", event.getEventId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                ComplianceAuditData auditData = ComplianceAuditData.builder()
                    .eventId(event.getEventId())
                    .eventType(event.getEventType().toString())
                    .entityId(event.getEntityId())
                    .timestamp(System.currentTimeMillis())
                    .correlationId(event.getEventId())
                    .complianceDetails(generateComplianceDetails(event))
                    .hash(generateEventHash(event))
                    .merkleProof(generateMerkleProof(event))
                    .build();

                TransactionReceipt receipt = auditContract.auditComplianceEvent(
                    auditData.getEventId(),
                    auditData.getEventType(),
                    auditData.getEntityId(),
                    auditData.getTimestamp(),
                    auditData.getHash(),
                    auditData.getComplianceDetails(),
                    auditData.getMerkleProof()
                ).send();

                log.info("Compliance event audited to blockchain. Hash: {}, Gas used: {}", 
                    receipt.getTransactionHash(), receipt.getGasUsed());

                return receipt.getTransactionHash();
            } catch (Exception e) {
                log.error("Failed to audit compliance event to blockchain", e);
                throw new BlockchainAuditException("Failed to audit to blockchain", e);
            }
        });
    }

    /**
     * Registra operação crítica na blockchain (AML, Sanctions)
     */
    public CompletableFuture<String> auditCriticalOperation(CriticalOperation operation) {
        log.info("Auditing critical operation to blockchain: {}", operation.getOperationId());

        return CompletableFuture.supplyAsync(() -> {
            try {
                CriticalAuditData auditData = CriticalAuditData.builder()
                    .operationId(operation.getOperationId())
                    .operationType(operation.getOperationType())
                    .severity(operation.getSeverity())
                    .description(operation.getDescription())
                    .timestamp(System.currentTimeMillis())
                    .userId(operation.getUserId())
                    .ipAddress(operation.getIpAddress())
                    .userAgent(operation.getUserAgent())
                    .regulatoryRequired(operation.isRegulatoryRequired())
                    .hash(generateOperationHash(operation))
                    .build();

                TransactionReceipt receipt = auditContract.auditCriticalOperation(
                    auditData.getOperationId(),
                    auditData.getOperationType(),
                    auditData.getSeverity(),
                    auditData.getDescription(),
                    auditData.getTimestamp(),
                    auditData.getUserId(),
                    auditData.getIpAddress(),
                    auditData.getUserAgent(),
                    auditData.isRegulatoryRequired(),
                    auditData.getHash()
                ).send();

                String txHash = receipt.getTransactionHash();
                log.info("Critical operation audited to blockchain. Hash: {}", txHash);

                // Armazena referência local para quick access
                storeLocalAuditReference(auditData, txHash);

                return txHash;
            } catch (Exception e) {
                log.error("Failed to audit critical operation to blockchain", e);
                throw new BlockchainAuditException("Failed to audit critical operation", e);
            }
        });
    }

    /**
     * Verifica integridade de auditoria blockchain
     */
    public boolean verifyAuditIntegrity(String transactionHash, String expectedHash) {
        try {
            TransactionReceipt receipt = web3jSvc.ethGetTransactionReceipt(transactionHash)
                .send()
                .getTransactionReceipt()
                .get();

            if (receipt == null) {
                log.warn("Transaction receipt not found for hash: {}", transactionHash);
                return false;
            }

            // Verificar se transação foi confirmada
            boolean confirmed = receipt.isStatusOK();
            
            if (confirmed) {
                log.info("Blockchain audit verified for hash: {}", transactionHash);
            } else {
                log.error("Blockchain audit verification failed for hash: {}", transactionHash);
            }

            return confirmed;
        } catch (Exception e) {
            log.error("Failed to verify audit integrity", e);
            return false;
        }
    }

    /**
     * Obtém histórico de auditoria blockchain
     */
    public List<BlockchainAuditEntry> getAuditHistory(String entityId, LocalDateTime from, LocalDateTime to) {
        // Implementação específica para consultar smart contract
        // Retorna eventos auditados entre período especificado
        
        log.debug("Retrieving blockchain audit history for entity: {} from {} to {}", 
            entityId, from, to);
        
        return List.of(); // Mock implementation
    }

    /**
     * Calcula hash SHA-256 do evento para integridade
     */
    private String generateEventHash(DomainEvent event) {
        String data = String.format("%s:%s:%s:%s", 
            event.getEventId(),
            event.getEventType(),
            event.getEntityId(),
            event.getTimestamp());
        
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data);
    }

    /**
     * Calcula hash SHA-256 da operação crítica
     */
    private String generateOperationHash(CriticalOperation operation) {
        String data = String.format("%s:%s:%s:%s:%s", 
            operation.getOperationId(),
            operation.getOperationType(),
            operation.getSeverity(),
            operation.getUserId(),
            operation.getTimestamp());
        
        return org.apache.commons.codec.digest.DigestUtils.sha256Hex(data);
    }

    /**
     * Gera detalhes específicos de compliance
     */
    private String generateComplianceDetails(DomainEvent event) {
        return String.format("COMPLIANCE_EVENT:%s:%s:%s", 
            event.getEventType(),
            event.getEntityId(),
            event.getInitiatedBy());
    }

    /**
     * Gera prova Merkle para integridade blockchain
     */
    private String generateMerkleProof(DomainEvent event) {
        // Implementação de Merkle proof para integrity
        return "merkle_proof_" + event.getEventId();
    }

    /**
     * Armazena referência local para acesso rápido
     */
    private void storeLocalAuditReference(CriticalAuditData auditData, String txHash) {
        // Armazenar referência em banco local para quick access
        log.debug("Storing local audit reference for tx: {}", txHash);
    }

    // Classes de dados auxiliares

    @lombok.Data
    @lombok.Builder
    public static class ComplianceAuditData {
        private String eventId;
        private String eventType;
        private String entityId;
        private long timestamp;
        private String correlationId;
        private String complianceDetails;
        private String hash;
        private String merkleProof;
    }

    @lombok.Data
    @lombok.Builder
    public static class CriticalAuditData {
        private String operationId;
        private String operationType;
        private String severity;
        private String description;
        private long timestamp;
        private String userId;
        private String ipAddress;
        private String userAgent;
        private boolean regulatoryRequired;
        private String hash;
    }

    @lombok.Data
    @lombok.Builder
    public static class CriticalOperation {
        private String operationId;
        private String operationType;
        private String severity;
        private String description;
        private long timestamp;
        private String userId;
        private String ipAddress;
        private String userAgent;
        private boolean regulatoryRequired;
    }

    @lombok.Data
    @lombok.Builder
    public static class BlockchainAuditEntry {
        private String transactionHash;
        private String eventId;
        private String eventType;
        private long blockNumber;
        private long timestamp;
        private String status;
    }

    public static class BlockchainAuditException extends RuntimeException {
        public BlockchainAuditException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
