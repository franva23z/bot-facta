# Usa uma imagem Java oficial que já vem com tudo configurado
FROM eclipse-temurin:17-jdk-jammy

# Define o diretório de trabalho
WORKDIR /app

# Copia os arquivos de configuração do Maven e o código
COPY . .

# Faz o build usando o Maven Wrapper
RUN ./mvnw clean package -DskipTests

# Move o JAR gerado para um nome fixo
RUN cp target/logica-0.0.1-SNAPSHOT.jar app.jar

# Define a porta (Render espera a porta 10000 por padrão, mas 8080 funciona)
EXPOSE 8080

# Comando para rodar
ENTRYPOINT ["java", "-jar", "app.jar"]