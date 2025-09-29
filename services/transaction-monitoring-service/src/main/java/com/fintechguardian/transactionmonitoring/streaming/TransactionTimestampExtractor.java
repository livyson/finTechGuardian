package com.fintechguardian.transactionmonitoring.streaming;

import com.fintechguardian.transactionmonitoring.entity.Transaction;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.streams.processor.TimestampExtractor;

/**
 * Timestamp extractor customizado para transações financeiras
 * Utiliza timestamp da transação para ordenação temporal correta
 */
public class TransactionTimestampExtractor implements TimestampExtractor {

    @Override
    public long extract(ConsumerRecord<Object, Object> record, long partitionTime) {
        try {
            if (record.value() instanceof Transaction) {
                Transaction transaction = (Transaction) record.value();
                
                // Usar timestamp da transação se disponível
                if (transaction.getTransactionDate() != null) {
                    return transaction.getTransactionDate()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                }
                
                // Fallback para processing date
                if (transaction.getProcessingDate() != null) {
                    return transaction.getProcessingDate()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli();
                }
            }
        } catch (Exception e) {
            // Log error but don't fail
            org.slf4j.LoggerFactory.getLogger(TransactionTimestampExtractor.class)
                    .warn("Error extracting timestamp from transaction: {}", e.getMessage());
        }
        
        // Fallback para partition time se não conseguir extrair
        return RECORD_TIMESTAMP_FALLBACK_TO_PARTITION;
    }
}
