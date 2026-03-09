package com.detona.logica;

import com.detona.controller.TelegramBotHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@Configuration
public class BotConfig {

    /**
     * Registra o bot no Telegram e inicia a sessão.
     * O Spring injeta automaticamente o seu TelegramBotHandler aqui.
     */
    @Bean
    public TelegramBotsApi telegramBotsApi(TelegramBotHandler telegramBotHandler) throws TelegramApiException {
        try {
            // Inicializa a API usando a sessão padrão
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            
            // Registra o bot que contém toda a lógica de abas (Wendy, Eliziene, Marlon)
            botsApi.registerBot(telegramBotHandler);
            
            return botsApi;
        } catch (TelegramApiException e) {
            // Log de erro caso o token ou a conexão falhem
            System.err.println("Erro ao registrar o bot: " + e.getMessage());
            throw e;
        }
    }
}