package com.detona.security;



import org.springframework.stereotype.Service;

import java.util.Map;



@Service

public class AcessoService {



    // Mapeamento de usuários e suas respectivas abas individuais

    private static final Map<String, String> USUARIOS = Map.of(

        "franvazxc", "tuanny",

        "wendyfv", "Wendy",

        "eli_torres16", "Eliziene",

        "marlonssilva", "marlon"

    );



    public boolean temAcesso(String username) {

        return username != null && USUARIOS.containsKey(username.toLowerCase());

    }



    public String obterAba(String username) {

        return USUARIOS.get(username.toLowerCase());

    }



    public boolean ehAdmin(String username) {

        

        return "franvazxc".equalsIgnoreCase(username);

    }

}