package com.detona.controller;

import com.detona.service.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
public class TelegramBotHandler extends TelegramLongPollingBot {

    private final GoogleSheetsService googleSheetsService;

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public TelegramBotHandler(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override
    public String getBotToken() { return this.botToken; }

    @Override
    public String getBotUsername() { return this.botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        // 1. TRATA MENSAGENS DE TEXTO (/proximo)
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            if (msg.equalsIgnoreCase("/proximo")) {
                enviarProximoComBotoes(chatId);
            } else {
                enviarResposta(chatId, "Olá Tuanny! Digite */proximo* para buscar o próximo cliente da planilha.");
            }
        } 
        
        // 2. TRATA OS CLIQUES NOS BOTÕES DE STATUS
        else if (update.hasCallbackQuery()) {
            String callData = update.getCallbackQuery().getData(); // Ex: "status:Agendado:5"
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            
            try {
                String[] partes = callData.split(":");
                String statusTexto = partes[1];
                int linha = Integer.parseInt(partes[2]);

                googleSheetsService.atualizarStatus(linha, statusTexto);
                
                enviarResposta(chatId, "✅ Status *'" + statusTexto + "'* gravado com sucesso na linha " + linha + "!\n\nDigite /proximo para o próximo.");
            } catch (Exception e) {
                enviarResposta(chatId, "❌ Erro ao gravar status: " + e.getMessage());
            }
        }
    }

    private void enviarProximoComBotoes(String chatId) {
        try {
            String info = googleSheetsService.getProximoCliente();
            
            // Verifica se o serviço retornou a linha para podermos criar os botões
            if (info.contains("Linha:")) {
                // Extrai o número da linha que o service enviou no final do texto
                int linha = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1).trim());
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(info);
                message.setParseMode("Markdown");

                // CONFIGURAÇÃO DOS BOTÕES DE STATUS
                InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
                List<List<InlineKeyboardButton>> rows = new ArrayList<>();

                // Linha 1 e 2
                rows.add(List.of(botao("1 - Não atende", "status:Não atende:"+linha), 
                                 botao("2 - Sem contato", "status:Sem contato:"+linha)));
                
                // Linha 3 e 4
                rows.add(List.of(botao("3 - Sem Op Disp", "status:Sem Op Disp:"+linha), 
                                 botao("4 - Sem interesse", "status:Sem interesse:"+linha)));
                
                // Linha 5 e 6
                rows.add(List.of(botao("5 - Agendado", "status:Agendado:"+linha), 
                                 botao("6 - Reagendado", "status:Reagendado:"+linha)));
                
                // Linha 7
                rows.add(List.of(botao("7 - Retornar Contato", "status:Retornar:"+linha)));

                markup.setKeyboard(rows);
                message.setReplyMarkup(markup);
                execute(message);
            } else {
                // Caso não tenha mais clientes ou erro, envia apenas o texto
                enviarResposta(chatId, info);
            }
        } catch (Exception e) {
            e.printStackTrace();
            enviarResposta(chatId, "❌ Erro ao processar comando: " + e.getMessage());
        }
    }

    private InlineKeyboardButton botao(String texto, String callback) {
        InlineKeyboardButton b = new InlineKeyboardButton();
        b.setText(texto);
        b.setCallbackData(callback);
        return b;
    }

    private void enviarResposta(String chatId, String texto) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(texto);
        message.setParseMode("Markdown");
        try {
            execute(message);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}