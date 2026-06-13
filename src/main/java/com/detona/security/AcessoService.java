package com.detona.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.Map;

@Service
public class AcessoService {

    @Value("#{${app.usuarios}}") 
    private Map<String, String> usuarios;

    @Value("${app.admin.username}") 
    private String adminUsername;

    public boolean temAcesso(String username) {
        return username != null && usuarios.containsKey(username.toLowerCase());
    }

    public String obterAba(String username) {
        return usuarios.get(username.toLowerCase());
    }

    public boolean ehAdmin(String username) {
        return adminUsername.equalsIgnoreCase(username);
    }
}