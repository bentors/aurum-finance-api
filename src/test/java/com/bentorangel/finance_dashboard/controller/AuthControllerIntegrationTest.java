package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.LoginDTO; // <-- Aqui está o nome correto!
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest// Sobe a API inteira de verdade (Controllers, Services, Banco de Dados)
@ActiveProfiles("test")
@AutoConfigureMockMvc // Cria o nosso "Postman/Swagger" interno
@Transactional // Ao final de cada teste, ele dá um rollback no banco, apagando tudo que o teste criou!
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc; // requisições HTTP

    @Autowired
    private UserRepository userRepository;

    private ObjectMapper objectMapper = new ObjectMapper(); // A ferramenta que transforma objetos Java em JSON

    @Test
    @DisplayName("Deve registrar um novo usuário com sucesso pelo endpoint HTTP")
    void register_Success() throws Exception {
        // Arrange: o objeto que seria digitado no front-end
        RegisterDTO registerDTO = new RegisterDTO("Bento Integração", "integracao@teste.com", "senha123");

        // Transformamos o objeto em uma String JSON
        String jsonPayload = objectMapper.writeValueAsString(registerDTO);

        // Act & Assert: requisição POST e verificamos o resultado
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isCreated()); // Esperamos que a API devolva um 201 OK
    }

    @Test
    @DisplayName("Deve fazer login e retornar um Token JWT pelo endpoint HTTP")
    void login_Success() throws Exception {
        // Arrange: Como o banco apaga tudo após cada teste, precisamos CRIAR um usuário primeiro
        RegisterDTO registerDTO = new RegisterDTO("Bento Login", "login@teste.com", "senha123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)));

        // Preparamos o JSON de Login
        LoginDTO loginDTO = new LoginDTO("login@teste.com", "senha123");
        String loginPayload = objectMapper.writeValueAsString(loginDTO);

        // Act & Assert: Tentamos logar
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isOk()) // Espera 200 OK
                .andExpect(jsonPath("$.token").exists()) // Espera que o JSON de resposta tenha um campo "token"
                .andExpect(jsonPath("$.token").isNotEmpty()); // Espera que o token não venha vazio!
    }

    @Test
    @DisplayName("Deve falhar ao tentar registrar um e-mail que já existe")
    void register_Fails_WhenEmailAlreadyExists() throws Exception {
        User existingUser = User.builder()
                .name("Usuário Original")
                .email("clone@teste.com")
                .password("senhaCriptografada")
                .active(true)
                .build();
        userRepository.save(existingUser);
        RegisterDTO dto = new RegisterDTO("Bento Clone", "clone@teste.com", "senha123");

        // requisição e esperamos um ERRO (400 Bad Request)
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("Deve retornar status 403 (Forbidden) ao tentar fazer login com senha incorreta")
    void login_Fails_WhenPasswordIsWrong() throws Exception {
        // Arrange: Cadastra o usuário válido
        RegisterDTO registerDTO = new RegisterDTO("Bento Seguro", "seguro@teste.com", "senha123");
        mockMvc.perform(post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(registerDTO)));

        // Preparamos o JSON de Login com a SENHA ERRADA
        LoginDTO loginDTO = new LoginDTO("seguro@teste.com", "hacker123");
        String loginPayload = objectMapper.writeValueAsString(loginDTO);

        // Act & Assert: Tentamos logar e o Spring Security DEVE bloquear a porta
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isForbidden()); // 403 Acesso Negado!
    }

    @Test
    @DisplayName("Deve retornar status 400 (Bad Request) ao tentar registrar com dados vazios")
    void register_Fails_WhenDataIsInvalid() throws Exception {
        // Arrange: Criamos um DTO com nome vazio e e-mail inválido
        // Obs: Assumindo que o seu RegisterDTO tem anotações como @NotBlank e @Email
        RegisterDTO registerDTO = new RegisterDTO("", "email-invalido", "");
        String jsonPayload = objectMapper.writeValueAsString(registerDTO);

        // Act & Assert: O Spring Validation deve interceptar e o seu GlobalExceptionHandler deve devolver 400
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonPayload))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Deve retornar status 403 (Forbidden) ao tentar logar com e-mail inexistente")
    void login_Fails_WhenEmailDoesNotExist() throws Exception {
        // Arrange: Preparamos um JSON com um e-mail que nunca foi salvo no banco
        LoginDTO loginDTO = new LoginDTO("fantasma@teste.com", "senha123");
        String loginPayload = objectMapper.writeValueAsString(loginDTO);

        // Act & Assert: O Spring Security deve barrar a entrada
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginPayload))
                .andExpect(status().isForbidden());
    }
}