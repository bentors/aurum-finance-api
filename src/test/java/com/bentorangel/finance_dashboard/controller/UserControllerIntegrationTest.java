package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.LoginDTO;
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UserControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Fábrica de Tokens com e-mail dinâmico para evitar conflitos!
    private String getAccessToken() throws Exception {
        String emailUnico = "vip" + System.currentTimeMillis() + "@teste.com";
        RegisterDTO registerDTO = new RegisterDTO("Bento VIP", emailUnico, "senha123");

        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)));

        LoginDTO loginDTO = new LoginDTO(emailUnico, "senha123");
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginDTO)))
                .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.token");
    }

    @Test
    @DisplayName("Deve barrar o acesso (403 Forbidden) se tentar ver o perfil sem Token")
    void getUserProfile_Fails_WhenNoToken() throws Exception {
        mockMvc.perform(get("/api/v1/users/me"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar os dados do usuário (200 OK) quando enviar um Token JWT válido")
    void getUserProfile_Success_WithValidToken() throws Exception {
        String token = getAccessToken();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                // Verifica se o nosso endpoint está retornando o nome correto!
                .andExpect(jsonPath("$.name").value("Bento VIP"))
                .andExpect(jsonPath("$.email").exists());
    }
}