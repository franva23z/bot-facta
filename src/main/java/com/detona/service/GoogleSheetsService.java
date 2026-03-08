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

    public String getProximoCliente() throws IOException {
       
        String range = "pagina1!A2:D2000"; 
        
        ValueRange response = sheetsService.spreadsheets().values()
                .get(spreadsheetId, range)
                .execute();

        List<List<Object>> values = response.getValues();

        if (values == null || values.isEmpty()) {
            return "Não encontrei dados na aba <b>pagina1</b>.";
        }

        for (int i = 0; i < values.size(); i++) {
            List<Object> row = values.get(i);
            
            
            if (row.size() < 4 || row.get(3) == null || row.get(3).toString().trim().isEmpty()) {
                
                
                String cpf = row.get(0).toString();
                String nome = (row.size() > 1) ? row.get(1).toString() : "Sem Nome"; 
                String telRaw = (row.size() > 2) ? row.get(2).toString() : "";     
                
               
                String telLimpo = telRaw.replaceAll("\\D", "");
                if (telLimpo.isEmpty()) continue;
                if (!telLimpo.startsWith("55")) telLimpo = "55" + telLimpo;

                int linhaPlanilha = i + 2;

               
                return "<b>📊 PRÓXIMO CLIENTE FACTA</b>\n\n" +
                       "<b>🆔 CPF:</b> " + cpf + "\n" +
                       "<b>👤 NOME:</b> " + nome + "\n" +
                       "<b>📞 TEL:</b> " + telRaw + "\n\n" +
                       "<a href=\"https://wa.me/" + telLimpo + "\"> CHAMAR</a>\n" +
                       "--------------------------\n" +
                       "Linha:" + linhaPlanilha;
            }
        }
        return "✅ Todos os clientes da <b>pagina1</b> já possuem status!";
    }
public String buscarClienteEspecifico(String termoBusca) throws IOException {
    // Busca o mesmo intervalo que você já definiu
    String range = "pagina1!A2:D2000"; 
    ValueRange response = sheetsService.spreadsheets().values()
            .get(spreadsheetId, range)
            .execute();

    List<List<Object>> values = response.getValues();

    if (values == null || values.isEmpty()) {
        return "Planilha vazia.";
    }

    for (int i = 0; i < values.size(); i++) {
        List<Object> row = values.get(i);
        
        // Verifica se a linha tem dados
        if (row.size() < 2) continue;

        String cpf = row.get(0).toString();
        String nome = row.get(1).toString();

        // Se o termo de busca bater com CPF ou parte do Nome
        if (cpf.equals(termoBusca) || nome.toLowerCase().contains(termoBusca.toLowerCase())) {
            
            String telRaw = (row.size() > 2) ? row.get(2).toString() : "";
            String telLimpo = telRaw.replaceAll("\\D", "");
            if (!telLimpo.startsWith("55") && !telLimpo.isEmpty()) telLimpo = "55" + telLimpo;

            int linhaPlanilha = i + 2;

            return "<b>🔍 CLIENTE ENCONTRADO (Linha " + linhaPlanilha + ")</b>\n\n" +
                   "<b>🆔 CPF:</b> " + cpf + "\n" +
                   "<b>👤 NOME:</b> " + nome + "\n" +
                   "<b>📞 TEL:</b> " + telRaw + "\n\n" +
                   "<a href=\"https://wa.me/" + telLimpo + "\">📱 CHAMAR NO WHATSAPP</a>";
        }
    }
    return "❌ Cliente não encontrado com o termo: " + termoBusca;
}
    public void atualizarStatus(int linha, String status) throws IOException {
        String range = "pagina1!D" + linha;
        ValueRange body = new ValueRange().setValues(List.of(List.of(status)));
        sheetsService.spreadsheets().values().update(spreadsheetId, range, body)
                .setValueInputOption("RAW").execute();
    }
}