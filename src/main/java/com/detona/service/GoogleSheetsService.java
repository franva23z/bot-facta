package com.detona.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.io.IOException;
import java.util.List;

@Service
public class GoogleSheetsService {

    private final Sheets sheetsService;

    @Value("${google.sheets.id}")
    private String spreadsheetId;

    public GoogleSheetsService(Sheets sheetsService) {
        this.sheetsService = sheetsService;
    }

    public String getProximoCliente(String aba) throws IOException {
        String range = aba + "!A2:E2000";
        ValueRange response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) return "❌ Aba " + aba + " vazia.";

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() < 4 || row.get(3) == null || row.get(3).toString().trim().isEmpty()) {
                return formatarSaida(row, i + 2, aba);
            }
        }
        return "✅ Todos os clientes de <b>" + aba + "</b> possuem status!";
    }

    public String buscarClienteEspecifico(String aba, String termo) throws IOException {
        String range = aba + "!A2:E2000";
        ValueRange response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();

        if (values == null) return "❌ Erro ao ler planilha.";

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            if (row.size() < 2) continue;
            if (row.get(0).toString().contains(termo) || row.get(1).toString().toLowerCase().contains(termo.toLowerCase())) {
                return formatarSaida(row, i + 2, aba);
            }
        }
        return "❌ Não encontrado na aba " + aba;
    }

    private String formatarSaida(List<Object> row, int linha, String aba) {
        String telRaw = (row.size() > 2) ? row.get(2).toString() : "";
        String telLimpo = telRaw.replaceAll("\\D", "");
        if (!telLimpo.isEmpty() && !telLimpo.startsWith("55")) telLimpo = "55" + telLimpo;

        return "<b>📊 CLIENTE (" + aba + ")</b>\n\n" +
               "<b>🆔 CPF:</b> " + row.get(0) + "\n" +
               "<b>👤 NOME:</b> " + row.get(1) + "\n" +
               "<b>📞 TEL:</b> " + telRaw + "\n\n" +
               "<a href=\"https://wa.me/" + telLimpo + "\">📱 CHAMAR AGORA</a>\n" +
               "--------------------------\n" +
               "Linha:" + linha;
    }

    public void atualizarStatus(String aba, int linha, String status) throws IOException {
        String range = aba + "!D" + linha;
        sheetsService.spreadsheets().values().update(spreadsheetId, range, 
            new ValueRange().setValues(List.of(List.of(status))))
            .setValueInputOption("RAW").execute();
    }

    public void atualizarTroco(String aba, int linha, String valor) throws IOException {
        String range = aba + "!E" + linha;
        sheetsService.spreadsheets().values().update(spreadsheetId, range, 
            new ValueRange().setValues(List.of(List.of(valor))))
            .setValueInputOption("RAW").execute();
    }

    public String listarSemOpcao(String aba) throws IOException {
        String range = aba + "!A2:D2000";
        ValueRange response = sheetsService.spreadsheets().values().get(spreadsheetId, range).execute();
        List<List<Object>> values = response.getValues();
        StringBuilder sb = new StringBuilder("<b>📋 REPESCAGEM (" + aba + ")</b>\n\n");
        int cont = 0;
        if (values != null) {
            for (int i = 0; i < values.size(); i++) {
                List<Object> row = values.get(i);
                if (row.size() >= 4 && row.get(3).toString().equalsIgnoreCase("Sem Op Disp")) {
                    sb.append("📍 L").append(i + 2).append(": ").append(row.get(1)).append("\n");
                    cont++; if (cont >= 15) break;
                }
            }
        }
        return cont == 0 ? "✅ Nada para repescagem em " + aba : sb.toString();
    }
}