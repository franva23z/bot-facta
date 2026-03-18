package com.detona.controller;

import com.detona.service.GoogleSheetsService;
import com.detona.security.AcessoService;
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
    private final AcessoService acessoService;
    private final Map<String, String> estadoTroco = new HashMap<>();

    @Value("${telegram.bot.token}") private String botToken;
    @Value("${telegram.bot.username}") private String botUsername;

    public TelegramBotHandler(GoogleSheetsService googleSheetsService, AcessoService acessoService) {
        this.googleSheetsService = googleSheetsService;
        this.acessoService = acessoService;
    }

    @Override public String getBotToken() { return this.botToken; }
    @Override public String getBotUsername() { return this.botUsername; }

    @Override
    public void onUpdateReceived(Update update) {
        String user = obterUsername(update);
        String chatId = obterChatId(update);

        if (!acessoService.temAcesso(user)) {
            if (update.hasMessage()) enviarResposta(chatId, "🚫 Acesso Negado para @" + user);
            return;
        }

        String aba = acessoService.obterAba(user);

        if (update.hasMessage() && update.getMessage().hasText()) {
            String msg = update.getMessage().getText();

            if (estadoTroco.containsKey(chatId)) {
                processarTroco(chatId, aba, msg, user);
                return;
            }

            if (msg.equalsIgnoreCase("/proximo")) processarEnvio(chatId, aba, "proximo", null);
            else if (msg.toLowerCase().startsWith("/buscar ")) processarEnvio(chatId, aba, "buscar", msg.substring(8).trim());
            else if (msg.equalsIgnoreCase("/listar")) enviarListaReagendados(chatId, aba);
            else if (msg.toLowerCase().startsWith("/limpar ")) processarLimpeza(chatId, aba, msg);
            else if (msg.equalsIgnoreCase("/painel") && acessoService.ehAdmin(user)) {
                enviarResposta(chatId, "📊 Painel de Admin em desenvolvimento... (Aba relatorio_de_clientes sendo alimentada!)");
            }

        } else if (update.hasCallbackQuery()) {
            processarCallback(update, aba, chatId);
        }
    }

    private void processarCallback(Update update, String aba, String chatId) {
        String data = update.getCallbackQuery().getData();
        String user = update.getCallbackQuery().getFrom().getUserName();
        
        try {
            if (data.startsWith("b:")) {
                processarEnvio(chatId, aba, "buscar", data.substring(2));
            } 
            else if (data.startsWith("s:")) {
                String[] p = data.split(":");
                String status = p[1];
                int linha = Integer.parseInt(p[2]);

                googleSheetsService.atualizarStatus(aba, linha, status);
                googleSheetsService.registrarNoRelatorioGeral(aba, linha, status, user);

                enviarResposta(chatId, "✅ Status <b>" + status + "</b> salvo!\n\n🎯 Digite /proximo para o próximo.");

                if (status.equalsIgnoreCase("Agendado")) {
                    estadoTroco.put(chatId, aba + ":" + linha);
                    enviarResposta(chatId, "💵 <b>Qual o valor do TROCO?</b>");
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void enviarListaReagendados(String chatId, String aba) {
        try {
            List<String> reagendados = googleSheetsService.obterListaReagendados(aba); 
            if (reagendados.isEmpty()) {
                enviarResposta(chatId, "✅ Nada pendente em " + aba);
                return;
            }
            InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();
            for (String nome : reagendados) { rows.add(List.of(botao(nome, "b:" + nome))); }
            markup.setKeyboard(rows);
            SendMessage sm = new SendMessage(chatId, "📋 <b>REAGENDADOS:</b>\nClique no nome para abrir:");
            sm.setParseMode("HTML"); sm.setReplyMarkup(markup);
            execute(sm);
        } catch (Exception e) { enviarResposta(chatId, "❌ Erro ao listar."); }
    }

    private void processarTroco(String chatId, String aba, String msg, String user) {
        try {
            String[] info = estadoTroco.get(chatId).split(":");
            int linha = Integer.parseInt(info[1]);
            googleSheetsService.atualizarTroco(aba, linha, msg);
            googleSheetsService.registrarNoRelatorioGeral(aba, linha, "Agendado (R$ "+msg+")", user);
            estadoTroco.remove(chatId);
            enviarResposta(chatId, "💰 Troco R$ " + msg + " salvo! Digite /proximo");
        } catch (Exception e) { enviarResposta(chatId, "❌ Erro no troco."); }
    }

    private void processarEnvio(String chatId, String aba, String tipo, String termo) {
        try {
            String info = tipo.equals("buscar") ? googleSheetsService.buscarClienteEspecifico(aba, termo) : googleSheetsService.getProximoCliente(aba);
            if (info.contains("Linha:")) {
                int linha = Integer.parseInt(info.substring(info.lastIndexOf(":") + 1).trim());
                SendMessage sm = new SendMessage(chatId, info);
                sm.setParseMode("HTML");
                sm.setReplyMarkup(criarBotoes(linha));
                execute(sm);
            } else enviarResposta(chatId, info);
        } catch (Exception e) { enviarResposta(chatId, "❌ Erro ao carregar."); }
    }

    private InlineKeyboardMarkup criarBotoes(int linha) {
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        rows.add(List.of(botao("1 - Não atende", "s:Não atende:"+linha), botao("2 - Sem contato", "s:Sem contato:"+linha)));
        rows.add(List.of(botao("3 - Sem Op Disp", "s:Sem Op Disp:"+linha), botao("4 - Sem interesse", "s:Sem interesse:"+linha)));
        rows.add(List.of(botao("5 - Agendado", "s:Agendado:"+linha), botao("6 - Reagendado", "s:Reagendado:"+linha)));
        rows.add(List.of(botao("7 - Retornar", "s:Retornar:"+linha), botao("8 - Msg Enviada", "s:Msg Enviada:"+linha)));
        markup.setKeyboard(rows);
        return markup;
    }

    private String obterUsername(Update u) {
        if (u.hasMessage()) return u.getMessage().getFrom().getUserName();
        if (u.hasCallbackQuery()) return u.getCallbackQuery().getFrom().getUserName();
        return null;
    }

    private String obterChatId(Update u) {
        if (u.hasMessage()) return u.getMessage().getChatId().toString();
        if (u.hasCallbackQuery()) return u.getCallbackQuery().getMessage().getChatId().toString();
        return null;
    }

    private InlineKeyboardButton botao(String t, String c) {
        InlineKeyboardButton b = new InlineKeyboardButton(); b.setText(t); b.setCallbackData(c); return b;
    }

    private void enviarResposta(String cid, String txt) {
        SendMessage s = new SendMessage(cid, txt); s.setParseMode("HTML");
        try { execute(s); } catch (Exception e) { e.printStackTrace(); }
    }

    private void processarLimpeza(String chatId, String aba, String msg) {
        try {
            int linha = Integer.parseInt(msg.substring(8).trim());
            googleSheetsService.atualizarStatus(aba, linha, "");
            googleSheetsService.atualizarTroco(aba, linha, "");
            enviarResposta(chatId, "♻️ Linha " + linha + " limpa.");
        } catch (Exception e) { enviarResposta(chatId, "❌ Use: /limpar [linha]"); }
    }
}