#!/bin/bash

# Script para construir e executar o FinTechGuardian em ambiente local
# Utiliza Java 23 e Virtual Threads para máximo desempenho

set -e

echo "🚀 FinTechGuardian - Build & Run Script"
echo "========================================"

# Verificar se Java 23 está disponível
check_java_version() {
    if command -v java &> /dev/null; then
        JAVA_VERSION=$(java --version 2>&1 | head -1 | cut -d' ' -f2)
        echo "📋 Java Version: $JAVA_VERSION"
        
        # Verificar se é Java 21+ (compatível com Java 23 features)
        MAJOR_VERSION=$(echo $JAVA_VERSION | cut -d'.' -f1)
        if [ "$MAJOR_VERSION" -lt 21 ]; then
            echo "❌ Java 21+ é necessário (encontrado: $JAVA_VERSION)"
            exit 1
        fi
    else
        echo "❌ Java não encontrado"
        exit 1
    fi
}

# Verificar dependências externas
check_dependencies() {
    echo "📋 Verificando dependências..."
    
    # Verificar Docker
    if command -v docker &> /dev/null; then
        echo "✅ Docker encontrado"
    else
        echo "❌ Docker não encontrado - necessário para infraestrutura"
        exit 1
    fi
    
    # Verificar Docker Compose
    if command -v docker-compose &> /dev/null; then
        echo "✅ Docker Compose encontrado"
    else
        echo "❌ Docker Compose não encontrado"
        exit 1
    fi
    
    # Verificar Maven
    if command -v mvn &> /dev/null; then
        MVN_VERSION=$(mvn --version | head -1 | cut -d' ' -f3)
        echo "✅ Maven encontrado (v$MVN_VERSION)"

    else
        echo "❌ Maven não encontrado"
        exit 1
    fi
}

# Iniciar infraestrutura com Docker Compose
start_infrastructure() {
    echo "🐳 Iniciando infraestrutura..."
    
    # Criar arquivo .env se não existir
    if [ ! -f .env ]; then
        echo "📝 Criando arquivo .env..."
        cp env.example .env
        echo "⚠️  Configure as senhas no arquivo .env antes de executar em produção"
    fi
    
    docker-compose up -d postgres mongodb redis kafka prometheus grafana
    
    echo "⏳ Aguardando serviços ficarem disponíveis..."
    
    # Aguardar PostgreSQL
    echo "🗄️  Aguardando PostgreSQL..."
    while ! docker-compose exec -T postgres pg_isready -U fintech_user -d fintechguardian &>/dev/null; do
        sleep 2
    done
    echo "✅ PostgreSQL está pronto"
    
    # Aguardar Redis
    echo "🔄 Aguardando Redis..."
    while ! docker-compose exec -T redis redis-cli ping &>/dev/null; do
        sleep 2
    done
    echo "✅ Redis está pronto"
    
    # Aguardar Kafka
    echo "📺 Aguardando Kafka..."
    while ! docker-compose logs kafka | tail -20 | grep -q "started"; do
        sleep 5
    done
    echo "✅ Kafka está pronto"
}

# Construir projeto Maven
build_project() {
    echo "🔨 Construindo projeto..."
    
    # Limpar e instalar dependências compartilhadas primeiro
    echo "📦 Instalando bibliotecas compartilhadas..."
    mvn clean install -pl libraries/common-domain -am
    mvn clean install -pl libraries/security-lib -am
    mvn clean install -pl libraries/kafka-producers-consumers -am
    
    # Construir todos os módulos
    echo "🔨 Construindo todos os módulos..."
    mvn clean install -DskipTests
    
    echo "✅ Build concluído com sucesso!"
}

# Executar serviços em modo desenvolvimento
run_services() {
    echo "🚀 Iniciando serviços..."
    
    # Iniciar Gateway Service em background
    echo "🌐 Iniciando Gateway Service..."
    mvn spring-boot:run -pl services/gateway-service -Dspring-boot.run.profiles=dev &
    GATEWAY_PID=$!
    
    sleep 10
    
    # Verificar se Gateway está funcionando
    echo "🔍 Verificando Gateway Service..."
    if curl -s http://localhost:8080/actuator/health &>/dev/null; then
        echo "✅ Gateway Service está rodando em http://localhost:8080"
    else
        echo "❌ Gateway Service falhou ao iniciar"
        exit 1
    fi
    
    echo ""
    echo "🎉 FinTechGuardian está rodando!"
    echo "📋 URLs importantes:"
    echo "   🌐 Gateway API: http://localhost:8080"
    echo "   📊 Swagger UI: http://localhost:8080/swagger-ui.html"
    echo "   📈 Prometheus: http://localhost:9090"
    echo "   📊 Grafana: http://localhost:3000 (admin/admin)"
    echo "   🎯 Kafka UI: http://localhost:8089"
    echo ""
    echo "Pressione Ctrl+C para parar todos os serviços"
    
    # Aguardar interrupção
    trap 'stop_services' INT
    wait
}

# Parar serviços
stop_services() {
    echo ""
    echo "🛑 Parando serviços..."
    
    if [ ! -z "$GATEWAY_PID" ]; then
        kill $GATEWAY_PID 2>/dev/null || true
    fi
    
    # Parar infraestrutura se solicitado
    if [ "$1" = "--stop-infra" ]; then
        echo "🐳 Parando infraestrutura..."
        docker-compose down
    fi
    
    echo "✅ Todos os serviços parados"
}

# Função principal
main() {
    check_java_version
    check_dependencies
    
    case "${1:-run}" in
        "infra-only")
            start_infrastructure
            echo "✅ Apenas infraestrutura iniciada. Execute './scripts/build-and-run.sh' para iniciar os serviços."
            ;;
        "build-only")
            build_project
            ;;
        "run")
            start_infrastructure
            build_project
            run_services
            ;;
        "stop")
            stop_services --stop-infra
            ;;
        *)
            echo "Uso: $0 [infra-only|build-only|run|stop]"
            echo ""
            echo "Comandos:"
            echo "  infra-only  - Inicia apenas infraestrutura (Docker)"
            echo "  build-only  - Apenas constrói o projeto"
            echo "  run         - Execução completa (padrão)"
            echo "  stop        - Para todos os serviços e infraestrutura"
            exit 1
            ;;
    esac
}

# Executar função principal
main "$@"
