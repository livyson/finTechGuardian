package com.fintechguardian.customerprofile.repository;

import com.fintechguardian.common.domain.enums.CustomerType;
import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.customerprofile.entity.Customer;
import com.fintechguardian.customerprofile.entity.Customer.CustomerStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Repository para operações com dados de clientes
 */
@Repository
public interface CustomerRepository extends JpaRepository<Customer, String> {

    /**
     * Busca cliente por CPF/CNPJ
     */
    Optional<Customer> findByDocumentNumberAndDocumentType(String documentNumber, Customer.DocumentType documentType);

    /**
     * Busca cliente por email
     */
    Optional<Customer> findByEmail(String email);

    /**
     * Verifica se documento já existe
     */
    boolean existsByDocumentNumberAndDocumentType(String documentNumber, Customer.DocumentType documentType);

    /**
     * Busca clientes por tipo
     */
    Page<Customer> findByCustomerType(CustomerType customerType, Pageable pageable);

    /**
     * Busca clientes por nível de risco
     */
    Page<Customer> findByRiskLevel(RiskLevel riskLevel, Pageable pageable);

    /**
     * Busca clientes por status
     */
    Page<Customer> findByStatus(CustomerStatus status, Pageable pageable);

    /**
     * Busca clientes PEP (Pessoas Expostas Politicamente)
     */
    @Query("SELECT c FROM Customer c WHERE c.pepFlag = true")
    List<Customer> findPepeCustomers();

    /**
     * Busca clientes com sanções
     */
    @Query("SELECT c FROM Customer c WHERE c.sanctionsScreeningResult IS NOT NULL AND c.sanctionsScreeningResult != ''")
    List<Customer> findCustomersWithSanctions();

    /**
     * Busca clientes com KYC expirado
     */
    @Query("SELECT c FROM Customer c WHERE c.kycExpiryDate < :today")
    List<Customer> findCustomersWithExpiredKyc(@Param("today") LocalDate today);

    /**
     * Busca clientes com KYC vencendo em breve
     */
    @Query("SELECT c FROM Customer c WHERE c.kycExpiryDate BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersWithExpiringKyc(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Busca clientes bloqueados
     */
    @Query("SELECT c FROM Customer c WHERE c.status = 'BLOCKED' OR c.blockedUntil > CURRENT_TIMESTAMP")
    List<Customer> findBlockedCustomers();

    /**
     * Busca clientes por filtros avançados
     */
    @Query("SELECT c FROM Customer c WHERE " +
           "(:name IS NULL OR LOWER(c.name) LIKE LOWER(CONCAT('%', :name, '%'))) AND " +
           "(:documentNumber IS NULL OR c.documentNumber LIKE CONCAT('%', :documentNumber, '%')) AND " +
           "(:customerType IS NULL OR c.customerType = :customerType) AND " +
           "(:riskLevel IS NULL OR c.riskLevel = :riskLevel) AND " +
           "(:status IS NULL OR c.status = :status) AND " +
           "(:pepFlag IS NULL OR c.pepFlag = :pepFlag)")
    Page<Customer> findCustomersWithFilters(@Param("name") String name,
                                          @Param("documentNumber") String documentNumber,
                                          @Param("customerType") CustomerType customerType,
                                          @Param("riskLevel") RiskLevel riskLevel,
                                          @Param("status") CustomerStatus status,
                                          @Param("pepFlag") Boolean pepFlag,
                                          Pageable pageable);

    /**
     * Busca clientes por faixa de renda
     */
    @Query("SELECT c FROM Customer c WHERE c.annualIncome BETWEEN :minIncome AND :maxIncome")
    List<Customer> findByAnnualIncomeBetween(@Param("minIncome") Double minIncome, @Param("maxIncome") Double maxIncome);

    /**
     * Conta clientes por status
     */
    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status")
    long countByStatus(@Param("status") CustomerStatus status);

    /**
     * Conta clientes por nível de risco
     */
    @Query("SELECT c.riskLevel, COUNT(c) FROM Customer c GROUP BY c.riskLevel")
    List<Object[]> countCustomersByRiskLevel();

    /**
     * Busca clientes criados em um período
     */
    List<Customer> findByCreatedAtBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Busca clientes por organização (para future multi-tenancy)
     */
    @Query("SELECT c FROM Customer c JOIN c.metadata metadata WHERE metadata LIKE %:organizationId%")
    List<Customer> findByOrganization(@Param("organizationId") String organizationId);

    /**
     * Busca clientes atualizados por usuário
     */
    List<Customer> findByUpdatedByAndUpdatedAtAfter(String updatedBy, LocalDate sinceDate);

    /**
     * Busca clientes com documentos próximos do vencimento
     */
    @Query("SELECT DISTINCT c FROM Customer c JOIN c.documents d WHERE d.expiryDate BETWEEN :startDate AND :endDate")
    List<Customer> findCustomersWithDocumentsExpiring(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Busca estatísticas de compliance por período
     */
    @Query("SELECT " +
           "COUNT(c) as totalCustomers, " +
           "SUM(CASE WHEN c.pepFlag = true THEN 1 ELSE 0 END) as pepCustomers, " +
           "SUM(CASE WHEN c.sanctionsScreeningResult IS NOT NULL THEN 1 ELSE 0 END) as sanctionedCustomers, " +
           "SUM(CASE WHEN c.kycExpiryDate < :today THEN 1 ELSE 0 END) as expiredKycCustomers " +
           "FROM Customer c WHERE c.createdAt BETWEEN :startDate AND :endDate")
    Object[] getComplianceStatistics(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate, @Param("today") LocalDate today);

    /**
     * Busca top clientes por volume de transações (requer join com service externo)
     */
    @Query(value = "SELECT c.* FROM customers c " +
                  "JOIN customer_statistics cs ON c.id = cs.customer_id " +
                  "ORDER BY cs.total_volume DESC " +
                  "LIMIT :limit", nativeQuery = true)
    List<Customer> findTopCustomersByVolume(@Param("limit") int limit);
}
