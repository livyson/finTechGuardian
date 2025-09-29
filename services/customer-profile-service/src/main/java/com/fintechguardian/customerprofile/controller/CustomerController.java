package com.fintechguardian.customerprofile.controller;

import com.fintechguardian.common.domain.enums.CustomerType;
import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.customerprofile.dto.CustomerDto;
import com.fintechguardian.customerprofile.entity.Customer.CustomerStatus;
import com.fintechguardian.customerprofile.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller REST para operações com clientes
 */
@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Management", description = "APIs para gestão de clientes")
@SecurityRequirement(name = "Bearer Authentication")
public class CustomerController {

    private final CustomerService customerService;

    /**
     * Lista clientes com filtros opcionais
     */
    @GetMapping
    @Operation(summary = "Listar clientes", description = "Busca clientes com filtros aplicáveis")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes retornada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Parâmetros de consulta inválidos"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<Page<CustomerDto>> listCustomers(
            @Parameter(description = "Nome para filtrar") @RequestParam(required = false) String name,
            @Parameter(description = "Número do documento") @RequestParam(required = false) String documentNumber,
            @Parameter(description = "Tipo de cliente") @RequestParam(required = false) CustomerType customerType,
            @Parameter(description = "Nível de risco") @RequestParam(required = false) RiskLevel riskLevel,
            @Parameter(description = "Status do cliente") @RequestParam(required = false) CustomerStatus status,
            @Parameter(description = "Cliente PEP") @RequestParam(required = false) Boolean pepFlag,
            @Parameter(description = "Configuração de paginação") Pageable pageable) {

        log.info("Listando clientes com filtros - Name: {}, DocumentNumber: {}, CustomerType: {}, RiskLevel: {}, Status: {}, PepFlag: {}",
                name, documentNumber, customerType, riskLevel, status, pepFlag);

        Page<CustomerDto> customers = customerService.findCustomersWithFilters(
                name, documentNumber, customerType, riskLevel, status, pepFlag, pageable);

        return ResponseEntity.ok(customers);
    }

    /**
     * Busca cliente por ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Buscar cliente por ID", description = "Retorna dados completos do cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<CustomerDto> getCustomer(@PathVariable String id) {
        log.info("Buscando cliente por ID: {}", id);

        return customerService.findById(id)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Busca cliente por documento
     */
    @GetMapping("/search/document")
    @Operation(summary = "Buscar cliente por documento", description = "Busca cliente por número e tipo de documento")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente encontrado"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<CustomerDto> getCustomerByDocument(
            @Parameter(description = "Número do documento") @RequestParam String documentNumber,
            @Parameter(description = "Tipo do documento") @RequestParam String documentType) {

        log.info("Buscando cliente por documento: {} / {}", documentNumber, documentType);

        return customerService.findByDocument(documentNumber, documentType)
                .map(customer -> ResponseEntity.ok(customer))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Cria novo cliente
     */
    @PostMapping
    @Operation(summary = "Criar cliente", description = "Cadastra novo cliente na plataforma")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "409", description = "Cliente já existe"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<CustomerDto> createCustomer(@Valid @RequestBody CustomerDto customerDto) {
        log.info("Criando novo cliente: {}", customerDto.getName());

        CustomerDto createdCustomer = customerService.createCustomer(customerDto);

        return ResponseEntity.status(HttpStatus.CREATED).body(createdCustomer);
    }

    /**
     * Atualiza cliente existente
     */
    @PutMapping("/{id}")
    @Operation(summary = "Atualizar cliente", description = "Atualiza dados do cliente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<CustomerDto> updateCustomer(
            @PathVariable String id,
            @Valid @RequestBody CustomerDto customerDto) {

        log.info("Atualizando cliente: {}", id);

        CustomerDto updatedCustomer = customerService.updateCustomer(id, customerDto);
        return ResponseEntity.ok(updatedCustomer);
    }

    /**
     * Bloqueia cliente
     */
    @PutMapping("/{id}/block")
    @Operation(summary = "Bloquear cliente", description = "Bloqueia cliente temporária ou permanentemente")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Cliente bloqueado com sucesso"),
        @ApiResponse(responseCode = "404", description = "Cliente não encontrado"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
   @{PreAuthorize"hasRole('COMPLIANCE_OFFICER') or hasRole('ADMIN')")
    public ResponseEntity<CustomerDto> blockCustomer(
            @PathVariable String id,
            @Parameter(description = "Motivo do bloqueio") @RequestParam String reason,
            @Parameter(description = "Data do fim do bloqueio (opcional)") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime blockedUntil) {

        log.warn("Bloqueando cliente: {} - Motivo: {}", id, reason);

        CustomerDto blockedCustomer = customerService.blockCustomer(id, reason, blockedUntil);
        return ResponseEntity.ok(blockedCustomer);
    }

    /**
     * Lista clientes PEP
     */
    @GetMapping("/pep")
    @Operation(summary = "Listar clientes PEP", description = "Retorna todos os clientes identificados como PEP")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes PEP"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<CustomerDto>> getPepeCustomers() {
        log.info("Listando clientes PEP");

        List<CustomerDto> pepCustomers = customerService.findPepeCustomers();
        return ResponseEntity.ok(pepCustomers);
    }

    /**
     * Lista clientes com sanções
     */
    @GetMapping("/sanctions")
    @Operation(summary = "Listar clientes com sanções", description = "Retorna clientes que aparecem em listas de sanções")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes com sanções"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<CustomerDto>> getCustomersWithSanctions() {
        log.info("Listando clientes com sanções");

        List<CustomerDto> sanctionedCustomers = customerService.findCustomersWithSanctions();
        return ResponseEntity.ok(sanctionedCustomers);
    }

    /**
     * Lista clientes com KYC expirado
     */
    @GetMapping("/kyc/expired")
    @Operation(summary = "Listar clientes com KYC expirado", description = "Retorna clientes cujo KYC está vencido")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de clientes com KYC expirado"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER')")
    public ResponseEntity<List<CustomerDto>> getCustomersWithExpiredKyc() {
        log.info("Listando clientes com KYC expirado");

        List<CustomerDto> expiredKycCustomers = customerService.findCustomersWithExpiredKyc();
        return ResponseEntity.ok(expiredKycCustomers);
    }

    /**
     * Estatísticas gerais de clientes
     */
    @GetMapping("/statistics")
    @Operation(summary = "Estatísticas de clientes", description = "Retorna estatísticas gerais dos clientes")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Estatísticas retornadas"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'ANALYST')")
    public ResponseEntity<CustomerStatisticsDto> getCustomerStatistics() {
        log.info("Gerando estatísticas de clientes");

        // Implementar geração de estatísticas
        CustomerStatisticsDto statistics = new CustomerStatisticsDto();
        // Estatísticas básicas de exemplo
        statistics.setTotalCustomers(1500L);
        statistics.setPepeCustomers(25L);
        statistics.setCustomersWithSanctions(5L);
        statistics.setExpiredKycCustomers(150L);

        return ResponseEntity.ok(statistics);
    }

    /**
     * DTO para estatísticas de clientes
     */
    @lombok.Data
    @Builder
    public static class CustomerStatisticsDto {
        private Long totalCustomers;
        private Long pepCustomers;
        private Long customersWithSanctions;
        private Long expiredKycCustomers;
        private Long activeCustomers;
        private Long blockedCustomers;
    }
}
