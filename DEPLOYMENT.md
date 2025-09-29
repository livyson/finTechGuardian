# FinTechGuardian - Guia de Deploy

## Visão Geral

Este documento fornece instruções detalhadas para deploy do FinTechGuardian em diferentes ambientes, desde desenvolvimento local até produção em Kubernetes.

## Pré-requisitos

- Java 23 ou superior
- Maven 3.9+
- Docker e Docker Compose
- Kubernetes cluster (para produção)
- kubectl configurado (para produção)

## Ambientes

### Desenvolvimento Local

```bash
# Execução rápida com Docker Compose
./scripts/build-and-run.sh

# Ou apenas infraestrutura
./scripts/build-and-run.sh infra-only

# Build e execução completa
./scripts/build-and-run.sh run
```

### Staging

```bash
# Configurar ambiente staging
kubectl apply -k kubernetes/overlays/dev

# Verificar deployment
kubectl get pods -n fintechguardian

# Logs dos serviços
kubectl logs -f deployment/gateway-service -n fintechguardian
```

### Produção

```bash
# Configurar secrets de produção
./scripts/create-production-secrets.sh

# Deploy em Kubernetes
kubectl apply -k kubernetes/overlays/prod

# Monitorar deployment
kubectl rollout status deployment/gateway-service -n fintechguardian

# Verificar health checks
kubectl get ingress -n fintechguardian
```

## Configuração por Ambiente

### Desenvolvimento (dev)

- Redis em memória única
- PostgreSQL sem replicação
- Kafka single-node
- Logs em DEBUG

### Kubernetes (kubernetes)

- Service Discovery via Kubernetes DNS
- ConfigMaps para configuração
- Secrets para dados sensíveis
- Resource limits definidos

### Produção (prod)

- Redis cluster com alta disponibilidade
- PostgreSQL com replicação master-slave
- Kafka cluster multi-broker
- Logs estruturados para SIEM
- Monitoramento completo com Prometheus/Grafana

## Health Checks

Todos os serviços expõem endpoints de health check:

- `/actuator/health/liveness` - Liveness probe
- `/actuator/health/readiness` - Readiness probe  
- `/actuator/prometheus` - Métricas Prometheus

## Monitoramento

### Métricas Disponíveis

- **Rate Limiting**: Requests bloqueados por IP
- **Circuit Breaker**: Estados dos circuit breakers
- **Authentication**: Tokens validados/rejeitados
- **Performance**: Latência e throughput por serviço

### Dashboards Grafana

1. **Gateway Overview**: Visão geral do API Gateway
2. **Service Health**: Health status de todos os serviços
3. **Authentication Metrics**: Métricas de autenticação
4. **Response Times**: Distribuição de tempos de resposta

### Alertas Configurados

- Gateway com latency > 2s
- Circuit breaker aberto por > 1 minuto
- Taxa de erro > 5%
- Uso de CPU > 80%
- Uso de memória > 90%

## Troubleshooting

### Problemas Comuns

1. **Gateway não inicia**
   ```bash
   # Verificar logs
   kubectl logs deployment/gateway-service -n fintechguardian
   
   # Verificar configuração
   kubectl describe deployment/gateway-service -n fintechguardian
   ```

2. **Autenticação falha**
   - Verificar JWT secret
   - Validar Role-Based Access Control
   - Conferir configuração OAuth2

3. **Circuit Breaker aberto**
   - Verificar saúde dos serviços downstream
   - Conferir timeouts configurados
   - Validar carregamento da infraestrutura

### Logs Úteis

```bash
# Logs do Gateway em tempo real
kubectl logs -f deployment/gateway-service -n fintechguardian

# Logs de eventos Kubernetes
kubectl get events -n fintechguardian --sort-by=.metadata.creationTimestamp

# Logs de todos os pods
kubectl logs -l app=gateway-service -n fintechguardian
```

## Performance Tuning

### JVM Tuning

```bash
# Para desenvolvimento
-Xms256m -Xmx512m -XX:+UseZGC -Dspring.threads.virtual.enabled=true

# Para produção
-Xms1g -Xmx4g -XX:+UseZGC -XX:+TieredCompilation -XX:TieredStopAtLevel=1 -Dspring.threads.virtual.enabled=true
```

### Kubernetes Resources

```yaml
resources:
  requests:
    memory: "256Mi"
    cpu: "100m"
  limits:
    memory: "1Gi" 
    cpu: "1000m"
```

## Segurança

### Configurações Importantes

1. **Rotação de Secrets**: Regelmäßige rotating JWT secrets
2. **Network Policies**: Isolare serviços em namespaces
3. **Pod Security Standards**: Implementar security contexts
4. **Ingress TLS**: Sempre usar HTTPS em produção

### Auditoria

Todos os acessos são registrados com:
- Timestamp preciso
- User ID e roles
- Organization ID
- IP de origem
- Endpoints acessados
- Status code da resposta

## Backup e Recuperação

### Banco de Dados

```bash
# Backup PostgreSQL
kubectl exec -it postgres-pod -- pg_dump fintechguardian > backup.sql

# Backup MongoDB
kubectl exec -it mongodb-pod -- mongodump --db fintechguardian
```

### Configurações

```bash
# Exportar ConfigMaps
kubectl get configmap -n fintechguardian -o yaml > configs-backup.yaml

# Exportar Secrets (sem valores)
kubectl get secret -n fintechguardian -o yaml > secrets-backup.yaml
```

## Updates e Versionamento

```bash
# Rolling update
kubectl set image deployment/gateway-service gateway-service=fintechguardian/gateway-service:v2.0.0 -n fintechguardian

# Rollback se necessário
kubectl rollout undo deployment/gateway-service -n fintechguardian
```

## Contatos de Suporte

- **Development Team**: dev@fintechguardian.com
- **DevOps Team**: devops@fintechguardian.com  
- **Security Team**: security@fintechguardian.com

Para emeregencias de produção, acesse o canal #fintechguardian-prod no Slack.
