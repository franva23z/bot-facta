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

import java.util.*;

@Component
public class TelegramBotHandler extends TelegramLongPollingBot {

    private final GoogleSheetsService googleSheetsService;
    private final Map<String, String> estadoTroco = new HashMap<>();

    @Value("${telegram.bot.token}") 
    private String botToken;

    @Value("${telegram.bot.username}") 
    private String botUsername;

    public TelegramBotHandler(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @Override public String getBotToken() { return this.botToken; }
    @Override public String getBotUsername() { return this.botUsername; }

    // MÉTODO IDENTIFICADOR COM TRAVA DE SEGURANÇA
    private String identificarAba(String user) {
        if (user == null) return null;
        return switch (user.toLowerCase()) {
            case "franvazxc" -> "pagina1";
            case "wendyfv" -> "Wendy";
            case "eli_torres16" -> "eliziene";
            case "marlonssilva" -> "marlon";
            default -> null; // Retorna null para usuários desconhecidos
        };
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();
            String chatId = update.getMessage().getChatId().toString();
            String user = update.getMessage().getFrom().getUserName();
            
            String aba = identificarAba(user);

            // VERIFICAÇÃO DE ACESSO
            if (aba == null) {
                enviarResposta(chatId, "🚫 <b>Acesso Negado</b>\nSeu usuário (@" + (user != null ? user : "desconhecido") + ") não está autorizado.");
                return;
            }

            // LÓGICA PARA GRAVAR O TROCO
            if (estadoTroco.containsKey(chatId)) {
                try {
                    String[] info = estadoTroco.get(chatId).split(":"); // aba:linha
                    googleSheetsService.atualizarTroco(info[0], Integer.parseInt(info[1]), msg);
                    estadoTroco.remove(chatId);
                    enviarResposta(chatId, "💰 Valor de <b>R$ " + msg + "</b> gravado na aba <b>" + aba + "</b>!");
                } catch (Exception e) {
                    enviarResposta(chatId, "❌ Erro ao gravar valor. Tente novamente.");
                }
                return;
            }

            // COMANDOS
            if (msg.equalsIgnoreCase("/proximo")) {
                processarEnvio(chatId, aba, "proximo", null);
            } 
            else if (msg.toLowerCase().startsWith("/buscar ")) {
                processarEnvio(chatId, aba, "buscar", msg.substring(8).trim());
            }
            else if (msg.equalsIgnoreCase("/repescagem")) {
                try {
                    enviarResposta(chatId, googleSheetsService.listarSemOpcao(aba));
                } catch (Exception e) { e.printStackTrace(); }
            }
            else if (msg.toLowerCase().startsWith("/limpar ")) {
                try {
                    int linha = Integer.parseInt(msg.substring(8).trim());
                    googleSheetsService.atualizarStatus(aba, linha, "");
                    googleSheetsService.atualizarTroco(aba, linha, "");
                    enviarResposta(chatId, "♻️ Linha <b>" + linha + "</b> limpa na aba <b>" + aba + "</b>.");
                } catch (Exception e) {
                    enviarResposta(chatId, "❌ Use: /limpar [número da linha]");
                }
            }
        } 
        else if (update.hasCallbackQuery()) {
            processarBotoes(update);
        }
    }

    private void processarBotoes(Update update) {
        String data = update.getCallbackQuery().getData();
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        String user = update.getCallbackQuery().getFrom().getUserName();
        String aba = identificarAba(user);

        if (aba == null) return; // Ignora se não for usuário autorizado

        try {
            String[] p = data.split(":"); // s:Status:Linha
            String status = p[1];
            int linha = Integer.parseInt(p[2]);

            googleSheetsService.atualizarStatus(aba, linha, status);
            enviarResposta(chatId, "✅ @" + user + " marcou <b>" + status + "</b> na linha " + linha);

            if (status.equalsIgnoreCase("Agendado")) {
                estadoTroco.put(chatId, aba + ":" + linha);
                enviarResposta(chatId, "💵 <b>Qual o valor do TROCO?</b>\n(Responda apenas com o número)");
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void processarEnvio(String chatId, String aba, String tipo, String termo) {
        try {
            String info = tipo.equals("buscar") ? 
                googleSheetsService.buscarClienteEspecifico(aba, termo) : 
                googleSheetsService.getProximoCliente(aba);

            if (info.contains("Linha:")) {
                int linha = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1).trim());
                SendMessage sm = new SendMessage(chatId, info);
                sm.setParseMode("HTML");
                sm.setReplyMarkup(criarBotoes(linha));
                execute(sm);
            } else {
                enviarResposta(chatId, info);
            }
        } catch (Exception e) { enviarResposta(chatId, "❌ Erro ao buscar dados."); }
    }

    private InlineKeyboardMarkup criarBotoes(int linha) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();

        rows.add(List.of(botao("1 - Não atende", "s:Não atende:"+linha), botao("2 - Sem contato", "s:Sem contato:"+linha)));
        rows.add(List.of(botao("3 - Sem Op Disp", "s:Sem Op Disp:"+linha), botao("4 - Sem interesse", "s:Sem interesse:"+linha)));
        rows.add(List.of(botao("5 - Agendado", "s:Agendado:"+linha), botao("6 - Reagendado", "s:Reagendado:"+linha)));
        rows.add(List.of(botao("7 - Retornar", "s:Retornar:"+linha)));

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
        SendMessage message = new SendMessage(chatId, texto);
        message.setParseMode("HTML");
        try { execute(message); } catch (TelegramApiException e) { e.printStackTrace(); }
    }
}