package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.CategoryRequestDTO;
import com.bentorangel.finance_dashboard.dto.LoginDTO;
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.bentorangel.finance_dashboard.model.CategoryType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
class CategoryControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private ObjectMapper objectMapper = new ObjectMapper();

    // Fábrica de Crachás
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
    @DisplayName("Deve barrar o acesso (403 Forbidden) ao tentar buscar categorias sem Token")
    void getCategories_Fails_WhenNoToken() throws Exception {
        // Tenta entrar na rota de categorias sem o Header
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve acessar as categorias (200 OK) quando enviar um Token JWT válido")
    void getCategories_Success_WithValidToken() throws Exception {
        String token = getAccessToken();

        // Entra na rota de categorias mostrando o crachá
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Deve criar uma categoria com sucesso (201 Created ou 200 OK)")
    void createCategory_Success() throws Exception {
        // 1. Pega o cracha
        String token = getAccessToken();

        // 2. Instancia o Record passando os valores no construtor
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("Salário", CategoryType.INCOME);

        String jsonPayload = objectMapper.writeValueAsString(requestDTO);

        // 3. Atira no Controller e verifica se salvou
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().is2xxSuccessful()) // Aceita 200 ou 201
                .andExpect(jsonPath("$.name").value("Salário"));
    }

    @Test
    @DisplayName("Deve retornar status 400 (Bad Request) ao tentar criar categoria sem nome")
    void createCategory_Fails_WhenNameIsBlank() throws Exception {
        String token = getAccessToken();

        // Criamos o Record com o nome vazio para forçar a ativação do @NotBlank
        CategoryRequestDTO requestDTO = new CategoryRequestDTO("", CategoryType.INCOME);

        String jsonPayload = objectMapper.writeValueAsString(requestDTO);

        // O GlobalExceptionHandler deve interceptar e retornar 400
        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve garantir isolamento: Usuário B não pode ver categorias do Usuário A")
    void getCategories_ReturnsOnlyUserCategories() throws Exception {
        // 1. Usuário A (VIP) loga e cria uma categoria
        String tokenA = getAccessToken();
        CategoryRequestDTO requestA = new CategoryRequestDTO("Salário VIP", CategoryType.INCOME);

        mockMvc.perform(post("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(requestA)))
                .andExpect(status().is2xxSuccessful());

        // 2. Criamos o Usuário B (Intruso) e pegamos o token dele
        RegisterDTO registerIntruso = new RegisterDTO("Intruso", "intruso@teste.com", "senha123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerIntruso)));

        LoginDTO loginIntruso = new LoginDTO("intruso@teste.com", "senha123");
        MvcResult resultIntruso = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginIntruso)))
                .andReturn();

        String tokenIntruso = JsonPath.read(resultIntruso.getResponse().getContentAsString(), "$.token");

        // 3. Usuário Intruso busca a lista de categorias dele
        // A lista DEVE voltar vazia, pois o "Salário VIP" pertence ao Usuário A!
        mockMvc.perform(get("/api/v1/categories")
                        .header("Authorization", "Bearer " + tokenIntruso))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }
}