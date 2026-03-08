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
        // 1. TRATA MENSAGENS DE TEXTO
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();

            if (msg.equalsIgnoreCase("/proximo")) {
                processarEnvioCliente(chatId, "proximo", null);
            } 
            else if (msg.toLowerCase().startsWith("/buscar ")) {
                String termo = msg.substring(8).trim();
                processarEnvioCliente(chatId, "buscar", termo);
            } 
            else {
                enviarResposta(chatId, "Olá Tuanny! \nUse */proximo* para o próximo da fila ou \n*/buscar NomeOuCPF* para localizar um cliente específico.");
            }
        } 
        
        // 2. TRATA OS CLIQUES NOS BOTÕES DE STATUS
        else if (update.hasCallbackQuery()) {
            String callData = update.getCallbackQuery().getData();
            String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
            
            try {
                String[] partes = callData.split(":");
                String statusTexto = partes[1];
                int linha = Integer.parseInt(partes[2]);

                googleSheetsService.atualizarStatus(linha, statusTexto);
                
                enviarResposta(chatId, "✅ Status *'" + statusTexto + "'* gravado na linha " + linha + "!\n\nDigite /proximo para continuar.");
            } catch (Exception e) {
                enviarResposta(chatId, "❌ Erro ao gravar status: " + e.getMessage());
            }
        }
    }

    /**
     * Método centralizado para buscar e enviar cliente com botões
     */
    private void processarEnvioCliente(String chatId, String acao, String termo) {
        try {
            String info;
            if (acao.equals("buscar")) {
                info = googleSheetsService.buscarClienteEspecifico(termo);
            } else {
                info = googleSheetsService.getProximoCliente();
            }

            // Se o retorno contém a indicação da linha, montamos o teclado
            if (info.contains("Linha:")) {
                // Extrai o número da linha (espera-se que esteja no final após o ':')
                int linha = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1).trim());
                
                SendMessage message = new SendMessage();
                message.setChatId(chatId);
                message.setText(info);
                message.setParseMode("HTML"); // Mudado para HTML para suportar os links <a href>

                InlineKeyboardMarkup markup = configurarBotoesStatus(linha);
                message.setReplyMarkup(markup);
                
                execute(message);
            } else {
                enviarResposta(chatId, info);
            }
        } catch (Exception e) {
            e.printStackTrace();
            enviarResposta(chatId, "❌ Erro ao processar: " + e.getMessage());
        }
    }

    /**
     * Cria o teclado de botões de status para uma linha específica
     */
    private InlineKeyboardMarkup configurarBotoesStatus(int linha) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(botao("1 - Não atende", "status:Não atende:"+linha), 
                         botao("2 - Sem contato", "status:Sem contato:"+linha)));
        
        rows.add(List.of(botao("3 - Sem Op Disp", "status:Sem Op Disp:"+linha), 
                         botao("4 - Sem interesse", "status:Sem interesse:"+linha)));
        
        rows.add(List.of(botao("5 - Agendado", "status:Agendado:"+linha), 
                         botao("6 - Reagendado", "status:Reagendado:"+linha)));
        
        rows.add(List.of(botao("7 - Retornar Contato", "status:Retornar:"+linha)));

        markup.setKeyboard(rows);
        return markup;
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