package com.detona.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleConfig {

    @Bean
    public Sheets googleSheets() throws IOException, GeneralSecurityException {
        // Tenta buscar na variável de ambiente (Padrão para o Railway)
        String jsonConfig = System.getenv("GOOGLE_CREDENTIALS");
        
        GoogleCredentials credentials;

        if (jsonConfig != null && !jsonConfig.isEmpty()) {
            credentials = GoogleCredentials.fromStream(new ByteArrayInputStream(jsonConfig.getBytes(StandardCharsets.UTF_8)))
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
        } else {
            // Caso rode local no VS Code
            try {
                org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("credentials.json");
                credentials = GoogleCredentials.fromStream(resource.getInputStream())
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            } catch (Exception e) {
                throw new IOException("❌ ERRO: Faltam as credenciais do Google!");
            }
        }

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("FactaAutomacaoBot") // Nome alterado para evitar travas
                .build();
    }
}