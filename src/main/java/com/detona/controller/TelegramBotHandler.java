package com.detona.controller;

import com.detona.service.GoogleSheetsService;
import com.detona.security.AcessoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.HashMap;
import java.util.Map;

@Component
public class TelegramBotHandler extends TelegramLongPollingBot {

    private final GoogleSheetsService googleSheetsService;
    private final AcessoService acessoService;
    private final Map<String, String> estadoTroco = new HashMap<>();

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.username}")
    private String botUsername;

    public TelegramBotHandler(
            GoogleSheetsService googleSheetsService,
            AcessoService acessoService) {

        this.googleSheetsService = googleSheetsService;
        this.acessoService = acessoService;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }

    @Override
    public void onUpdateReceived(Update update) {

        String user = obterUsername(update);
        String chatId = obterChatId(update);

        if (user == null || !acessoService.temAcesso(user)) {
            if (update.hasMessage()) {
                enviarResposta(chatId, "🚫 Acesso Negado.");
            }
            return;
        }

        String aba = acessoService.obterAba(user);

        if (update.hasMessage() && update.getMessage().hasText()) {

            String msg = update.getMessage().getText();

            if (estadoTroco.containsKey(chatId)) {
                processarTroco(chatId, aba, msg, user);
                return;
            }

            if (msg.equalsIgnoreCase("/proximo")) {

                processarEnvio(chatId, aba, "proximo", "");

            } else if (msg.toLowerCase().startsWith("/buscar ")) {

                processarEnvio(
                        chatId,
                        aba,
                        "buscar",
                        msg.substring(8).trim());

            } else if (msg.equalsIgnoreCase("/painel")
                    && acessoService.ehAdmin(user)) {

                enviarResposta(chatId, "📊 Painel de Admin ativo.");
            }
        }
    }

    private String obterUsername(Update update) {

        if (update.hasMessage()) {
            return update.getMessage().getFrom().getUserName();
        }

        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getFrom().getUserName();
        }

        return null;
    }

    private String obterChatId(Update update) {

        if (update.hasMessage()) {
            return update.getMessage().getChatId().toString();
        }

        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery()
                    .getMessage()
                    .getChatId()
                    .toString();
        }

        return null;
    }

    private void processarTroco(
            String chatId,
            String aba,
            String msg,
            String user) {

        try {

            googleSheetsService.atualizarTroco(
                    aba,
                    Integer.parseInt(
                            estadoTroco.get(chatId).split(":")[1]),
                    msg);

            estadoTroco.remove(chatId);

            enviarResposta(
                    chatId,
                    "💰 Troco salvo: R$ " + msg);

        } catch (Exception e) {

            e.printStackTrace();

            enviarResposta(
                    chatId,
                    "❌ Erro ao processar troco.");
        }
    }

    private void processarEnvio(
            String chatId,
            String aba,
            String tipo,
            String termo) {

        try {

            String info;

            if ("buscar".equals(tipo)) {

                info = googleSheetsService
                        .buscarClienteEspecifico(aba, termo);

            } else {

                info = googleSheetsService
                        .getProximoCliente(aba);
            }

            enviarResposta(chatId, info);

        } catch (Exception e) {

            e.printStackTrace();

            enviarResposta(
                    chatId,
                    "❌ Erro ao acessar a planilha.");
        }
    }

    private void enviarResposta(String chatId, String texto) {

        try {

            SendMessage mensagem =
                    new SendMessage(chatId, texto);

            mensagem.setParseMode("HTML");

            execute(mensagem);

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
}