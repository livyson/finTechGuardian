# Kubernetes - FinTechGuardian

## Arquitetura Kubernetes

O FinTechGuardian é deployado como um conjunto de microsserviços utilizando Kubernetes com os seguintes componentes:

### Namespace
Todos os recursos do FinTechGuardian são deployados no namespace `fintechguardian`:

```bash
kubectl create namespace fintechguardian
```

### Configuração de Rede

#### Ingress
Utiliza Nginx Ingress Controller com TLS termination:

```yaml
apiVersion: networking.k8s.io/v1
kind: Ingress
metadata:
  name: fintechguardian-ingress
  namespace: fintechguardian
  annotations:
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/cors-allow-origin: "https://app.fintechguardian.com"
    nginx.ingress.kubernetes.io/rate-limit: "100"
spec:
  tls:
  - hosts:
    - api.fintechguardian.com
    secretName: fintechguardian-tls
  rules:
  - host: api.fintechguardian.com
    http:
      paths:
      - path: /
        pathType: Prefix
        backend:
          service:
            name: gateway-service
            port:
              number: 80
```

#### Network Policies
Isolamento de tráfego entre pods:

```yaml
apiVersion: networking.k8s.io/v1
kind: NetworkPolicy
metadata:
  name: default-deny-all
  namespace: fintechguardian
spec:
  podSelector: {}
  policyTypes:
  - Ingress
  - Egress
```

### Armazenamento Persistente

#### PostgreSQL PVC
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: postgres-data
  namespace: fintechguardian
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 50Gi
  storageClassName: fast-ssd
```

#### MongoDB PVC
```yaml
apiVersion: v1
kind: PersistentVolumeClaim
metadata:
  name: mongodb-data
  namespace: fintechguardian
spec:
  accessModes:
    - ReadWriteOnce
  resources:
    requests:
      storage: 100Gi
  storageClassName: fast-ssd
```

### Configuração de Segurança

#### SecurityContext
```yaml
securityContext:
  runAsNonRoot: true
  runAsUser: 1000
  runAsGroup: 1000
  fsGroup: 1000
  fsGroupChangePolicy: "OnRootMismatch"
```

#### Pod Security Standards
Implementando Pod Security Standards restritivos:

```yaml
apiVersion: v1
kind: Namespace
metadata:
  name: fintechguardian
  labels:
    pod-security.kubernetes.io/enforce: restricted
    pod-security.kubernetes.io/audit: restricted
    pod-security.kubernetes.io/warn: restricted
```

### Autoscaling

#### HorizontalPodAutoscaler
```yaml
apiVersion: autoscaling/v2
kind: HorizontalPodAutoscaler
metadata:
  name: gateway-service-hpa
  namespace: fintechguardian
spec:
  scaleTargetRef:
    apiVersion: apps/v1
    kind: Deployment
    name: gateway-service
  minReplicas: 3
  maxReplicas: 20
  metrics:
  - type: Resource
    resource:
      name: cpu
      target:
        type: Utilization
        averageUtilization: 70
  - type: Resource
    resource:
      name: memory
      target:
        type: Utilization
        averageUtilization: 80
  - type: Pods
    pods:
      metric:
        name: http_requests_per_second
      target:
        type: AverageValue
        averageValue: "100"
```

### ServiceDiscovery

#### CoreDNS Configuration
Todos os serviços são descobertos via DNS interno do Kubernetes:

- `gateway-service.fintechguardian.svc.cluster.local`
- `customer-profile-service.fintechguardian.svc.cluster.local`
- `transaction-monitoring-service.fintechguardian.svc.cluster.local`
- `postgres-service.fintechguardian.svc.cluster.local`

### Monitoramento

#### ServiceMonitor (Prometheus)
```yaml
apiVersion: monitoring.coreos.com/v1
kind: ServiceMonitor
metadata:
  name: fintechguardian-services
  namespace: fintechguardian
  labels:
    app: fintechguardian
spec:
  selector:
    matchLabels:
      monitoring: enabled
  endpoints:
  - port: actuator
    path: /actuator/prometheus
    interval: 30s
```

#### PrometheusRule (Alerting)
```yaml
apiVersion: monitoring.coreos.com/v1
kind: PrometheusRule
metadata:
  name: fintechguardian-rules
  namespace: fintechguardian
spec:
  groups:
  - name: fintechguardian.rules
    rules:
    - alert: HighErrorRate
      expr: |
        (
          sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) /
          sum(rate(http_server_requests_seconds_count[5m]))
        ) > 0.05
      for: 5m
      labels:
        severity: warning
      annotations:
        summary: High error rate detected
```

### ConfigMaps e Secrets

#### ConfigMap Principal
```yaml
apiVersion: v1
kind: ConfigMap
metadata:
  name: app-config
  namespace: fintechguardian
data:
  application.yml: |
    spring:
      profiles:
        active: kubernetes
      cloud:
        kubernetes:
          discovery:
            enabled: true
    security:
      jwt:
        expiration-ms: 3600000
        refresh-expiration-ms: 604800000
```

#### Secrets
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: app-secrets
  namespace: fintechguardian
type: Opaque
data:
  jwt-secret: <base64-encoded>
  postgres-password: <base64-encoded>
  mongo-password: <base64-encoded>
  redis-password: <base64-encoded>
```

### Deploy com Kubernetes

#### Kustomization Base
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: fintechguardian

resources:
  - namespace.yaml
  - rbac.yaml
  - network-policies.yaml
  - configmap.yaml
  - secret.yaml
  - postgres-deployment.yaml
  - mongodb-deployment.yaml
  - redis-deployment.yaml
  - kafka-deployment.yaml
  - gateway-deployment.yaml
  - customer-profile-deployment.yaml
  - transaction-monitoring-deployment.yaml
  - compliance-case-deployment.yaml
  - regulatory-reporting-deployment.yaml
  - risk-engine-deployment.yaml
  - ingress.yaml
  - monitoring.yaml
```

#### Overlays por Ambiente
##### Desenvolvimento (`overlays/dev`)
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: fintechguardian-dev

resources:
  - ../../base

patches:
  - target:
      kind: Deployment
      name: gateway-service
    patch: |-
      - op: replace
        path: /spec/replicas
        value: 1
```

##### Produção (`overlays/prod`)
```yaml
apiVersion: kustomize.config.k8s.io/v1beta1
kind: Kustomization

namespace: fintechguardian

resources:
  - ../../base

patches:
  - target:
      kind: Deployment
    patch: |-
      - op: add
        path: /spec/template/spec/securityContext
        value:
          runAsNonRoot: true
          runAsUser: 1000

patchesJson6902:
  - target:
      kind: Deployment
    path: production-tuning.patch.json

secretGenerator:
  - name: app-secrets
    literals:
      - jwt-secret=<production-jwt-secret>
```
