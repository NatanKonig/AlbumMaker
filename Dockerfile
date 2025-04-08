FROM maven:3.8-openjdk-8-slim AS build

WORKDIR /app
COPY pom.xml .
# Baixar todas as dependências para não precisar baixá-las novamente se apenas o código mudar
RUN mvn dependency:go-offline

# Copiar o código fonte e compilar
COPY src ./src
RUN mvn package -DskipTests

# Usar uma imagem mínima para executar
FROM openjdk:8-jre-alpine

WORKDIR /app
# Copiar apenas o JAR compilado da fase de build
COPY --from=build /app/target/AlbumMaker-1.0-SNAPSHOT.jar .

# Primeiro criar diretório de logs
RUN mkdir -p /app/logs

# Depois adicionar um usuário não-root e dar permissões
RUN addgroup -S appgroup && \
    adduser -S appuser -G appgroup && \
    chown -R appuser:appgroup /app

# Mudar para o usuário não-root DEPOIS de criar os diretórios e configurar permissões
USER appuser

# Executar a aplicação
CMD ["java", "-jar", "AlbumMaker-1.0-SNAPSHOT.jar"]