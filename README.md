# 📊 Finance Dashboard API

Uma API RESTful robusta e segura para gestão de finanças pessoais, desenvolvida com Java 21 e Spring Boot. Este projeto serve como o motor ("backend") de um painel financeiro (Dashboard) que permite aos usuários gerenciar receitas, despesas, categorias personalizadas e visualizar resumos em tempo real.

## 🚀 Tecnologias e Arquitetura

Este projeto foi construído focando em padrões da indústria, alta performance e segurança corporativa:

* **Java 21 & Spring Boot 3.x:** Base sólida e moderna do projeto.
* **PostgreSQL:** Banco de dados relacional escolhido para confiabilidade.
* **Flyway:** Versionamento de banco de dados (Migrations) garantindo a integridade do schema em qualquer ambiente.
* **Spring Security & JWT:** Autenticação Stateless segura.
* **Multi-tenancy (Isolamento de Dados):** Arquitetura desenhada para que cada usuário acesse estritamente os seus próprios dados financeiros.
* **Spring Cache:** Implementado em rotas de alta demanda (como buscas de categorias) para respostas em milissegundos e economia de processamento do banco de dados.
* **Bucket4j:** Rate Limiting implementado para proteger a API contra ataques de força bruta e DDoS (limite de requisições por IP).
* **Spring Boot Actuator:** Endpoints de observabilidade e monitoramento de saúde do servidor (`/actuator/health`).
* **Testes Automatizados:** Ampla cobertura utilizando **JUnit 5, Mockito e MockMvc** para testes unitários de regras de negócio e testes de integração de ponta a ponta.
* **Swagger/OpenAPI 3:** Documentação viva e interativa dos endpoints.

## ⚙️ Principais Funcionalidades

- **Autenticação:** Registro de usuários, Login (geração de Token JWT) e validação de perfil atual (`/me`).
- **Gestão de Categorias:** CRUD completo de categorias de receitas e despesas, com validação de nomes duplicados por usuário.
- **Transações:** Lançamento de entradas e saídas atreladas às categorias, com suporte a paginação.
- **Dashboard:** Endpoint otimizado para consolidação financeira (Total de Receitas, Total de Despesas e Saldo Atual).

## 🛠️ Como executar o projeto localmente

### Pré-requisitos
- Java 21
- Maven
- PostgreSQL rodando localmente (ou via Docker)

### Passos
1. Clone o repositório:
   ```bash
   git clone [https://github.com/bentors/finance-dashboard.git](https://github.com/bentors/finance-dashboard.git)
   ```
2. Crie um banco de dados no PostgreSQL (ex: `financas_db`).
3. Configure as variáveis de ambiente na sua máquina ou na sua IDE:
    - `DB_URL` (ex: `jdbc:postgresql://localhost:5432/financas_db`)
    - `DB_USERNAME`
    - `DB_PASSWORD`
    - `JWT_SECRET` (Uma chave secreta para assinar os tokens)
4. Rode a aplicação via Maven:
   ```bash
   ./mvnw spring-boot:run
   ```
5. O Flyway criará todas as tabelas automaticamente na inicialização.

## 📚 Documentação da API
Com a aplicação rodando, acesse a interface interativa do Swagger para testar os endpoints:
👉 http://localhost:8081/swagger-ui.html

---

## 👨‍💻 Autor

**Bento Rangel**
- GitHub: [@bentors](https://github.com/bentors)
- LinkedIn: [Bento Rangel](https://www.linkedin.com/in/bento-rangel)
- Email: bento.rangel05@gmail.com

---

⭐ Se este projeto te ajudou de alguma forma, considere dar uma estrela!

**Desenvolvido com ☕ e Java por Bento Rangel**