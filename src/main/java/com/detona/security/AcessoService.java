package com.detona.security;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Service;
import java.util.HashMap;
import java.util.Map;

@Service
@ConfigurationProperties(prefix = "app")
public class AcessoService {

    private Map<String, String> usuarios = new HashMap<>();
    private String adminUsername;

    public Map<String, String> getUsuarios() { return usuarios; }
    public void setUsuarios(Map<String, String> usuarios) { this.usuarios = usuarios; }

    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }

    public boolean temAcesso(String username) {
        return username != null && usuarios.containsKey(username.toLowerCase());
    }

    public String obterAba(String username) {
        return usuarios.get(username.toLowerCase());
    }

    public boolean ehAdmin(String username) {
        return adminUsername != null && adminUsername.equalsIgnoreCase(username);
    }
}