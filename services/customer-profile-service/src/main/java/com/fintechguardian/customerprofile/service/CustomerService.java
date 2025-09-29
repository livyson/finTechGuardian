package com.fintechguardian.customerprofile.service;

import com.fintechguardian.common.domain.enums.CustomerType;
import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.common.domain.events.CustomerEvent;
import com.fintechguardian.common.domain.exception.ComplianceException;
import com.fintechguardian.customerprofile.dto.CustomerDto;
import com.fintechguardian.customerprofile.entity.Address;
import com.fintechguardian.customerprofile.entity.Customer;
import com.fintechguardian.customerprofile.entity.CustomerNote;
import com.fintechguardian.customerprofile.entity.CustomerDocument;
import com.fintechguardian.customerprofile.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transactions.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço principal para operações com clientes
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ComplianceService complianceService;
    private final RiskAssessmentService riskAssessmentService;

    private static final String CUSTOMER_EVENTS_TOPIC = "customer-events";

    /**
     * Cria um novo cliente
     */
    public CustomerDto createCustomer(CustomerDto customerDto) {
        log.info("Criando novo cliente com documento: {}", customerDto.getDocumentNumber());

        // Validação de documento único
        Customer.DocumentType docType = Customer.DocumentType.valueOf(customerDto.getDocumentType());
        if (customerRepository.existsByDocumentNumberAndDocumentType(customerDto.getDocumentNumber(), docType)) {
            throw new IllegalArgumentException("Cliente com documento " + customerDto.getDocumentNumber() + " já existe");
        }

        // Validação de email único (se fornecido)
        if (customerDto.getEmail() != null && !customerDto.getEmail().isEmpty()) {
            Optional<Customer> existingByEmail = customerRepository.findByEmail(customerDto.getEmail());
            if (existingByEmail.isPresent()) {
                throw new IllegalArgumentException("Cliente com email " + customerDto.getEmail() + " já existe");
            }
        }

        // Criar entidade
        Customer customer = mapDtoToEntity(customerDto);
        customer.setId(UUID.randomUUID().toString());
        customer.setCreatedAt(LocalDateTime.now());
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setStatus(Customer.CustomerStatus.ACTIVE);

        // Executar verificações de compliance
        performComplianceChecks(customer);

        // Calcular risco inicial
        RiskLevel riskLevel = riskAssessmentService.calculateInitialRiskLevel(customer);
        customer.setRiskLevel(riskLevel);

        // Salvar
        Customer savedCustomer = customerRepository.save(customer);

        // Publicar evento
        publishCustomerEvent(CustomerEvent.customerCreated(
            savedCustomer.getId(),
            savedCustomer.getDocumentNumber(),
            savedCustomer.getCustomerType()
        ));

        log.info("Cliente criado com sucesso: {}", savedCustomer.getId());
        return mapEntityToDto(savedCustomer);
    }

    /**
     * Busca cliente por ID
     */
    @Transactional(readOnly = true)
    public Optional<CustomerDto> findById(String id) {
        return customerRepository.findById(id)
                .map(this::mapEntityToDto);
    }

    /**
     * Busca cliente por documento
     */
    @Transactional(readOnly = true)
    public Optional<CustomerDto> findByDocument(String documentNumber, String documentType) {
        Customer.DocumentType docType = Customer.DocumentType.valueOf(documentType);
        return customerRepository.findByDocumentNumberAndDocumentType(documentNumber, docType)
                .map(this::mapEntityToDto);
    }

    /**
     * Atualiza dados do cliente
     */
    public CustomerDto updateCustomer(String id, CustomerDto customerDto) {
        log.info("Atualizando cliente: {}", id);

        Customer existingCustomer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));

        // Validações de atualização
        validateUpdatePermissions(existingCustomer, customerDto);

        // Mapear dados atualizados
        Customer updatedCustomer = mapDtoToEntity(customerDto);
        updatedCustomer.setId(id);
        updatedCustomer.setCreatedAt(existingCustomer.getCreatedAt());
        updatedCustomer.setUpdatedAt(LocalDateTime.now());
        updatedCustomer.setUpdatedBy(getCurrentUserId()); // Implementar autenticação
        updatedCustomer.setVersion(existingCustomer.getVersion());

        // Recalcular risco se dados relevantes mudaram
        if (shouldRecalculateRisk(existingCustomer, updatedCustomer)) {
            RiskLevel newRiskLevel = riskAssessmentService.recalculateRiskLevel(existingCustomer, updatedCustomer);
            RiskLevel oldRiskLevel = existingCustomer.getRiskLevel();
            updatedCustomer.setRiskLevel(newRiskLevel);

            if (!newRiskLevel.equals(oldRiskLevel)) {
                publishRiskAssessmentEvent(id, newRiskLevel, oldRiskLevel);
            }
        } else {
            updatedCustomer.setRiskLevel(existingCustomer.getRiskLevel());
        }

        Customer savedCustomer = customerRepository.save(updatedCustomer);

        // Publicar evento de atualização
        publishCustomerEvent(CustomerEvent.customerUpdated(id));

        log.info("Cliente atualizado com sucesso: {}", id);
        return mapEntityToDto(savedCustomer);
    }

    /**
     * Bloqueia cliente temporariamente ou permanentemente
     */
    public CustomerDto blockCustomer(String id, String reason, LocalDateTime blockedUntil) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Cliente não encontrado: " + id));

        customer.setStatus(Customer.CustomerStatus.BLOCKED);
        customer.setStatusReason(reason);
        customer.setBlockedUntil(blockedUntil);
        customer.setUpdatedAt(LocalDateTime.now());
        customer.setUpdatedBy(getCurrentUserId());

        Customer savedCustomer = customerRepository.save(customer);

        // Log da ação
        log.warn("Cliente bloqueado: {} - Motivo: {}", id, reason);

        return mapEntityToDto(savedCustomer);
    }

    /**
     * Busca clientes com filtros
     */
    @Transactional(readOnly = true)
    public Page<CustomerDto> findCustomersWithFilters(String name, String documentNumber,
                                                     CustomerType customerType, RiskLevel riskLevel,
                                                     Customer.CustomerStatus status, Boolean pepFlag,
                                                     Pageable pageable) {
        Page<Customer> customers = customerRepository.findCustomersWithFilters(
                name, documentNumber, customerType, riskLevel, status, pepFlag, pageable);

        return customers.map(this::mapEntityToDto);
    }

    /**
     * Busca clientes PEP
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> findPEECustomers() {
        List<Customer> customers = customerRepository.findPEECustomers();
        return customers.stream()
                .map(this::mapEntityToDto)
                .toList();
    }

    /**
     * Busca clientes com sanções
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> findCustomersWithSanctions() {
        List<Customer> customers = customerRepository.findCustomersWithSanctions();
        return customers.stream()
                .map(this::mapEntityToDto)
                .toList();
    }

    /**
     * Busca clientes com KYC expirado
     */
    @Transactional(readOnly = true)
    public List<CustomerDto> findCustomersWithExpiredKyc() {
        List<Customer> customers = customerRepository.findCustomersWithExpiredKyc(LocalDate.now());
        return customers.stream()
                .map(this::mapEntityToDto)
                .toList();
    }

    /**
     * Executa verificações de compliance para um cliente
     */
    private void performComplianceChecks(Customer customer) {
        try {
            // Verificação de sanções
            String sanctionsResult = complianceService.checkSanctionsList(customer);
            if (sanctionsResult != null && !sanctionsResult.isEmpty()) {
                publishComplianceEvent(CustomerEvent.sanctionsChecked(customer.getId(), sanctionsResult));
            }

            // Verificação PEP
            if (complianceService.isPEP(customer)) {
                customer.setPepFlag(true);
                publishComplianceEvent(CustomerEvent.pepIdentified(customer.getId()));
            }

            // Outras verificações de compliance
            complianceService.performKYCChecks(customer);

        } catch (ComplianceException e) {
            log.error("Erro na verificação de compliance para cliente {}: {}", customer.getId(), e.getMessage());
            throw e;
        }
    }

    /**
     * Publica evento de cliente no Kafka
     */
    private void publishCustomerEvent(CustomerEvent event) {
        try {
            kafkaTemplate.send(CUSTOMER_EVENTS_TOPIC, event.getEntityId(), event);
            log.debug("Evento de cliente publicado: {} - {}", event.getEventType(), event.getEntityId());
        } catch (Exception e) {
            log.error("Erro ao publicar evento de cliente: {}", e.getMessage());
            // Não falha a operação principal por erro de evento
        }
    }

    /**
     * Publica evento de compliance
     */
    private void publishComplianceEvent(CustomerEvent event) {
        publishCustomerEvent(event);
    }

    /**
     * Publica evento de mudança de risco
     */
    private void publishRiskAssessmentEvent(String customerId, RiskLevel newRiskLevel, RiskLevel oldRiskLevel) {
        CustomerEvent event = CustomerEvent.riskAssessed(
                customerId,
                newRiskLevel,
                oldRiskLevel != null ? oldRiskLevel.name() : null,
                "Recálculo automático após atualização de dados"
        );
        publishCustomerEvent(event);
    }

    // Métodos auxiliares...

    private boolean shouldRecalculateRisk(Customer oldCustomer, Customer newCustomer) {
        return !java.util.Objects.equals(oldCustomer.getName(), newCustomer.getName()) ||
               !java.util.Objects.equals(oldCustomer.getNationality(), newCustomer.getNationality()) ||
               !java.util.Objects.equals(oldCustomer.getOccupation(), newCustomer.getOccupation()) ||
               !java.util.Objects.equals(oldCustomer.getAnnualIncome(), newCustomer.getAnnualIncome()) ||
               !java.util.Objects.equals(oldCustomer.getPepFlag(), newCustomer.getPepFlag());
    }

    private String getCurrentUserId() {
        // Implementar obtenção do usuário atual do contexto de segurança
        return "system"; // Placeholder
    }

    private void validateUpdatePermissions(Customer existingCustomer, CustomerDto customerDto) {
        // Implementar validações de permissão baseadas em roles
        // Por exemplo, apenas certas roles podem alterar PEP flag, etc.
    }

    // Métodos de mapeamento (simplificados - idealmente usar MapStruct)
    private Customer mapDtoToEntity(CustomerDto dto) {
        // Implementar mapeamento completo
        return Customer.builder().build(); // Placeholder
    }

    private CustomerDto mapEntityToDto(Customer entity) {
        // Implementar mapeamento completo
        return CustomerDto.builder().build(); // Placeholder
    }
}
