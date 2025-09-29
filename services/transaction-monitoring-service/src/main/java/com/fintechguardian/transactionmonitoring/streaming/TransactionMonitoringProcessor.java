package com.fintechguardian.transactionmonitoring.streaming;

import com.fintechguardian.common.domain.events.TransactionEvent;
import com.fintechguardian.transactionmonitoring.entity.Transaction;
import com.fintechguardian.transactionmonitoring.service.TransactionAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.kstream.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.serializer.JsonSerde;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

/**
 * Kafka Streams Processor para monitoramento de transações em tempo real
 * Implementa detecção de padrões AML, análise comportamental e agregações
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class TransactionMonitoringProcessor {

    private final TransactionAnalysisService transactionAnalysisService;
    
    // Configurações de janelas de tempo
    private static final Duration WINDOW_SIZE = Duration.ofMinutes(5);
    private static final Duration WINDOW_GRACE_PERIOD = Duration.ofMinutes(1);
    
    @Bean
    public KStream<String, Transaction> processTransactions(StreamsBuilder streamsBuilder) {
        
        // Stream principal de transações ingressantes
        KStream<String, Transaction> transactionsStream = streamsBuilder
                .stream("transaction-events", 
                       Consumed.with(Serdes.String(), new JsonSerde<>(Transaction.class))
                               .withTimestampExtractor(new TransactionTimestampExtractor()));
        
        // Pre-processamento e enriquecimento
        KStream<String, Transaction> enrichedStream = transactionsStream
                .filter((key, transaction) -> transaction != null)
                .peek((key, transaction) -> log.debug("Processing transaction: {}", transaction.getId()))
                .mapValues(transactionAnalysisService::enrichTransaction);
        
        // Branch para diferentes análises paralelas
        Map<String, KStream<String, Transaction>> branches = enrichedStream
                .split(Named.as("branch"))
                .branch((key, transaction) -> isHighValue(transaction), Branched.as("high-value"))
                .branch((key, transaction) -> isInternationalTransaction(transaction), Branched.as("international"))
                .branch((key, transaction) -> isP2PTransaction(transaction), Branched.as("p2p"))
                .defaultBranch(Branched.as("standard"));
        
        // Processamento para diferentes tipos de transação
        processHighValueTransactions(branches.get("branchhigh-value"));
        processInternationalTransactions(branches.get("branchinternational"));
        processP2PTransactions(branches.get("branchp2p"));
        processStandardTransactions(branches.get("branch-standard"));
        
        // Agregações em tempo real por cliente
        processCustomerAggregations(enrichedStream);
        
        // Detecção de padrões suspeitos
        processSuspeiciousPatternDetection(enrichedStream);
        
        return enrichedStream;
    }
    
    /**
     * Processa transações de alto valor
     */
    private void processHighValueTransactions(KStream<String, Transaction> highValueStream) {
        highValueStream
                .foreach((key, transaction) -> {
                    log.warn("HIGH VALUE TRANSACTION DETECTED: {} - Amount: {}", 
                            transaction.getId(), transaction.getAmount());
                    
                    // Disparar análise de risco imediata
                    transactionAnalysisService.performInstantRiskAnalysis(transaction);
                    
                    // Enviar para alertas em tempo real
                    transactionAnalysisService.sendRealTimeAlert(transaction, "HIGH_VALUE_TRANSACTION");
                });
    }
    
    /**
     * Processa transações internacionais
     */
    private void processInternationalTransactions(KStream<String, Transaction> internationalStream) {
        internationalStream
                .groupByKey()
                .windowedBy(TimeWindows.of(WINDOW_SIZE).grace(WINDOW_SIZE))
                .aggregate(
                        InternationalTransactionAggregate::new,
                        (key, transaction, aggregate) -> {
                            aggregate.addTransaction(transaction);
                            return aggregate;
                        },
                        Materialized.with(Serdes.String(), new JsonSerde<>(InternationalTransactionAggregate.class))
                )
                .toStream()
                .filter((windowedKey, aggregate) -> aggregate.getTransactionCount() >= 3)
                .foreach((key, aggregate) -> {
                    log.warn("SUSPICIOUS INTERNATIONAL PATTERN: {} transactions in window", 
                            aggregate.getTransactionCount());
                    
                    // Marcar para investigação
                    transactionAnalysisService.triggerInvestigation(
                            aggregate.getTransactions(), "MULTIPLE_INTERNATIONAL_TRANSACTIONS");
                });
    }
    
    /**
     * Processa transações P2P
     */
    private void processP2PTransactions(KStream<String, Transaction> p2PStream) {
        p2PStream
                .groupBy((key, transaction) -> transaction.getCustomerId())
                .windowedBy(TimeWindows.of(Duration.ofMinutes(15)).grace(Duration.ofMinutes(2)))
                .aggregate(
                        P2PAggregate::new,
                        (key, transaction, aggregate) -> aggregate.addTransaction(transaction),
                        Materialized.with(Serdes.String(), new JsonSerde<>(P2PAggregate.class))
                )
                .toStream()
                .filter((key, aggregate) -> aggregate.getTotalAmount().compareTo(new java.math.BigDecimal("100000")) >= 0)
                .foreach((key, aggregate) -> {
                    log.warn("HIGH VOLUME P2P PATTERN: Customer {} - Amount: {}", 
                            key.key(), aggregate.getTotalAmount());
                    
                    transactionAnalysisService.performBehavioralAnalysis(
                            key.key(), aggregate.getTransactions());
                });
    }
    
    /**
     * Processa transações padrão
     */
    private void processStandardTransactions(KStream<String, Transaction> standardStream) {
        standardStream
                .mapValues(transaction -> {
                    // Análise básica para transações padrão
                    transactionAnalysisService.performBasicRiskScoring(transaction);
                    return transaction;
                })
                .to("analyzed-transactions", Produced.with(Serdes.String(), new JsonSerde<>(Transaction.class)));
    }
    
    /**
     * Processa agregações por cliente
     */
    private void processCustomerAggregations(KStream<String, Transaction> stream) {
        // Agregação de volume por cliente em janelas de 1 hora
        stream.groupBy((key, transaction) -> transaction.getCustomerId())
                .windowedBy(TimeWindows.of(Duration.ofHours(1)).grace(Duration.ofMinutes(5)))
                .aggregate(
                        CustomerTransactionAggregate::new,
                        (key, transaction, aggregate) -> aggregate.addTransaction(transaction),
                        Materialized.with(Serdes.String(), new JsonSerde<>(CustomerTransactionAggregate.class))
                )
                .toStream()
                .filter((key, aggregate) -> aggregate.getSuspiciousActivity())
                .foreach((key, aggregate) -> {
                    log.warn("SUSPICIOUS CUSTOMER ACTIVITY: Customer {} - Pattern: {}", 
                            key.key(), aggregate.getSuspiciousReason());
                    
                    transactionAnalysisService.triggerCustomerReview(key.key(), aggregate);
                });
    }
    
    /**
     * Detecta padrões suspeitos em tempo real
     */
    private void processSuspeiciousPatternDetection(KStream<String, Transaction> stream) {
        // Detecção de structuring em tempo real
        stream.groupBy((key, transaction) -> transaction.getCustomerId())
                .windowedBy(TimeWindows.of(Duration.ofMinutes(10)).grace(Duration.ofMinutes(2)))
                .aggregate(
                        StructuringDetector::new,
                        (key, transaction, detector) -> detector.processTransaction(transaction),
                        Materialized.with(Serdes.String(), new JsonSerde<>(StructuringDetector.class))
                )
                .toStream()
                .filter((key, detector) -> detector.isStructuringDetected())
                .foreach((key, detector) -> {
                    log.error("STRUCTURING PATTERN DETECTED: Customer {} - Details: {}", 
                            key.key(), detector.getDetailing());
                    
                    transactionAnalysisService.createComplianceCase(
                            key.key(), "STRUCTURING_PATTERN", detector.getSuspicioTransactions());
                });
    }
    
    // Métodos auxiliares para branch conditions
    
    private boolean isHighValue(Transaction transaction) {
        return transaction.getAmount().compareTo(new java.math.BigDecimal("50000")) >= 0;
    }
    
    private boolean isInternationalTransaction(Transaction transaction) {
        return transaction.getCounterpartyCountry() != null && 
               !transaction.getCounterpartyCountry().equals("BR");
    }
    
    private boolean isP2PTransaction(Transaction transaction) {
        return transaction.getTransactionType() == Transaction.TransactionType.TRANSFER &&
               transaction.getCounterpartyDocument() != null;
    }
    
    // Classes internas para agregações
    
    @lombok.Data
    public static class InternationalTransactionAggregate {
        private int transactionCount = 0;
        private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        private java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        
        public InternationalTransactionAggregate addTransaction(Transaction transaction) {
 thistransactionCount++;
            this.totalAmount = this.totalAmount.add(transaction.getAmount());
            this.transactions.add(transaction);
            return this;
        }
        
        public int getTransactionCount() { return transactionCount; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public java.util.List<Transaction> getTransactions() { return transactions; }
    }
    
    @lombok.Data
    public static class P2PAggregate {
        private int transactionCount = 0;
        private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        private java.util.List<Transaction> transactions = new java.util.ArrayList<>();
        
        public P2PAggregate addTransaction(Transaction transaction) {
            this.transactionCount++;
            this.totalAmount = this.totalAmount.add(transaction.getAmount());
            this.transactions.add(transaction);
            return this;
        }
        
        public int getTransactionCount() { return transactionCount; }
        public java.math.BigDecimal getTotalAmount() { return totalAmount; }
        public java.util.List<Transaction> getTransactions() { return transactions; }
    }
    
    @lombok.Data
    public static class CustomerTransactionAggregate {
        private int transactionCount = 0;
        private java.math.BigDecimal totalAmount = java.math.BigDecimal.ZERO;
        private java.util.Set<String> destinations = new java.util.HashSet<>();
        private boolean suspiciousActivity = false;
        private String suspiciousReason = "";
        
        public CustomerTransactionAggregate addTransaction(Transaction transaction) {
            this.transactionCount++;
            this.totalAmount = this.totalAmount.add(transaction.getAmount());
            if (transaction.getCounterpartyAccount() != null) {
                this.destinations.add(transaction.getCounterpartyAccount());
            }
            
            // Detectar atividade suspeita
            if (this.destinations.size() > 10) {
                this.suspiciousActivity = true;
                this.suspiciousReason = "Multiple destinations (" + this.destinations.size() + ")";
            }
            
            return this;
        }
        
        public boolean getSuspiciousActivity() { return suspiciousActivity; }
        public String getSuspiciousReason() { return suspiciousReason; }
    }
    
    @lombok.Data
    public static class StructuringDetector {
        private java.util.Map<java.math.BigDecimal, Integer> amountFrequency = new java.util.HashMap<>();
        private java.util.List<Transaction> suspiciousTransactions = new java.util.ArrayList<>();
        
        public StructuringDetector processTransaction(Transaction transaction) {
            java.math.BigDecimal amount = transaction.getAmount();
            amountFrequency.merge(amount, 1, Integer::sum);
            suspiciousTransactions.add(transaction);
            return this;
        }
        
        public boolean isStructuringDetected() {
            return amountFrequency.values().stream().anyMatch(count -> count >= 3);
        }
        
        public String getDetectionDetails() {
            return amountFrequency.entrySet().stream()
                    .filter(entry -> entry.getValue() >= 3)
                    .map(entry -> String.format("Amount $%.2f: %d transactions", entry.getKey().doubleValue(), entry.getValue()))
                    .reduce((s1, s2) -> s1 + "; " + s2)
                    .orElse("");
        }
        
        public java.util.List<Transaction> getSuspicioTransactions() { return suspiciousTransactions; }
    }
}
