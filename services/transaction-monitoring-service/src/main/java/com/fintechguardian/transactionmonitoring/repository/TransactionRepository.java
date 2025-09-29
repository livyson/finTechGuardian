package com.fintechguardian.transactionmonitoring.repository;

import com.fintechguardian.transactionmonitoring.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repositório para operações de dados das transações monitoradas
 */
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {

    /**
     * Busca transações por cliente
     */
    List<Transaction> findByCustomerIdOrderByTransactionDateDesc(String customerId);

    /**
     * Busca transações aprovadas automaticamente
     */
    List<Transaction> findByAutoApprovedTrueOrderByTransactionDateDesc();

    /**
     * Busca transações que requerem aprovação
     */
    List<Transaction> findByRequiresApprovalTrueAndApprovedByIsNullOrderByTransactionDateDesc();

    /**
     * Busca transações suspeitas por pontuação de risco
     */
    @Query("SELECT t FROM Transaction t WHERE t.riskScore >= :threshold AND t.status = 'COMPLETED' ORDER BY t.riskScore DESC")
    List<Transaction> findSuspiciousTransactions(@Param("threshold") BigDecimal threshold);

    /**
     * Busca transações internacionais
     */
    @Query("SELECT t FROM Transaction t WHERE t.counterpartyCountry != 'BR' AND t.counterpartyCountry IS NOT NULL ORDER BY t.transactionDate DESC")
    List<Transaction> findInternationalTransactions();

    /**
     * Busca transações de alto valor
     */
    @Query("SELECT t FROM Transaction t WHERE t.amount >= :amount ORDER BY t.amount DESC")
    List<Transaction> findHighValueTransactions(@Param("amount") BigDecimal threshold);

    /**
     * Agregação de volume diário por cliente
     */
    @Query("SELECT t.customerId, SUM(t.amount), COUNT(t) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "GROUP BY t.customerId")
    List<Object[]> findDailyVolumeSummary(@Param("startDate") LocalDateTime startDate, 
                                          @Param("endDate") LocalDateTime endDate);

    /**
     * Busca transações processadas em um período específico
     */
    @Query("SELECT t FROM Transaction t WHERE t.processingDate >= :startTime AND t.processingDate <= :endTime ORDER BY t.processingDate DESC")
    List<Transaction> findTransactionsProcessedBetween(@Param("startTime") LocalDateTime startTime, 
                                                       @Param("endTime") LocalDateTime endTime);

    /**
     * Contagem de transações por tipo em um período
     */
    @Query("SELECT t.transactionType, COUNT(t) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "GROUP BY t.transactionType")
    List<Object[]> countTransactionsByType(@Param("startDate") LocalDateTime startDate, 
                                           @Param("endDate") LocalDateTime endDate);

    /**
     * Busca transações com padrões estruturados (valores múltiplos de 1000)
     */
    @Query("SELECT t FROM Transaction t WHERE MOD(t.amount * 100, 100000) = 0 AND t.transactionDate >= :startDate ORDER BY t.transactionDate DESC")
    List<Transaction> findAllRoundAmountTransactions(@Param("startDate") LocalDateTime startDate);

    /**
     * Análise de frequência de transações por cliente
     */
    @Query("SELECT t.customerId, COUNT(t) as transactionCount, SUM(t.amount) as totalAmount " +
           "FROM Transaction t WHERE t.transactionDate >= :startDate " +
           "GROUP BY t.customerId " +
           "HAVING COUNT(t) >= :minFrequency " +
           "ORDER BY transactionCount DESC")
    List<Object[]> findHighFrequencyCustomers(@Param("startDate") LocalDateTime startDate, 
                                              @Param("minFrequency") Long minFrequency);

    /**
     * Busca transações rejeitadas com motivo específico
     */
    @Query("SELECT t FROM Transaction t WHERE t.status = 'REJECTED' AND t.rejectionReason LIKE %:keyword% ORDER BY t.transactionDate DESC")
    List<Transaction> findRejectedTransactionsByReason(@Param("keyword") String keyword);

    /**
     * Busca transações que falharam na verificação de sanções
     */
    @Query("SELECT t FROM Transaction t WHERE t.sanctionsChecked = false OR t.sanctionsChecked IS NULL ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsWithUncheckedSanctions();

    /**
     * Busca transações com análise de PEP pendente
     */
    @Query("SELECT t FROM Transaction t WHERE t.pepScreened = false OR t.pepScreened IS NULL ORDER BY t.transactionDate DESC")
    List<Transaction> findTransactionsWithPendingPepScreening();

    /**
     * Estatísticas de risco por período
     */
    @Query("SELECT t.riskLevel, COUNT(t), AVG(t.riskScore) FROM Transaction t " +
           "WHERE t.transactionDate >= :startDate AND t.transactionDate <= :endDate " +
           "GROUP BY t.riskLevel")
    List<Object[]> findRiskStatisticsByPeriod(@Param("startDate") LocalDateTime startDate, 
                                               @Param("endDate") LocalDateTime endDate);

    /**
     * Transações com tempo de processamento longo
     */
    @Query("SELECT t FROM Transaction t WHERE t.processingDate IS NOT NULL " +
           "AND (TIMESTAMPDIFF(MINUTE, t.transactionDate, t.processingDate) > :maxProcessingMinutes) " +
           "ORDER BY (TIMESTAMPDIFF(MINUTE, t.transactionDate, t.processingDate)) DESC")
    List<Transaction> findTransactionsWithLongProcessingTime(@Param("maxProcessingMinutes") Long maxProcessingMinutes);

    /**
     * Busca transações detectadas por análise comportamental
     */
    @Query("SELECT t FROM Transaction t WHERE t.suspiciousPatterns IS NOT NULL AND t.suspiciousPatterns != '' ORDER BY t.transactionDate DESC")
    List<Transaction> findBehavioralAnalysisFlaggedTransactions();
}
