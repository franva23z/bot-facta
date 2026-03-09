package com.detona.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Configuration
public class GoogleConfig {

    @Bean
    public Sheets googleSheets() throws IOException, GeneralSecurityException {
        
        // Certifique-se de que o arquivo credentials.json está em src/main/resources
        ClassPathResource resource = new ClassPathResource("credentials.json");
        
        if (!resource.exists()) {
            throw new IOException("❌ ERRO: O arquivo credentials.json não foi encontrado em src/main/resources!");
        }

        // Configura as credenciais com escopo de leitura e escrita em planilhas
        GoogleCredentials credentials = GoogleCredentials.fromStream(resource.getInputStream())
                .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));

        // Constrói o serviço do Google Sheets
        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Bot-Facta-Automacao") // Nome atualizado do seu bot
                .build();
    }
}