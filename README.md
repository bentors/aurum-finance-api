<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" />
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.5-6DB33F?style=flat-square&logo=springboot" />
  <img src="https://img.shields.io/badge/PostgreSQL-17-336791?style=flat-square&logo=postgresql" />
  <img src="https://img.shields.io/badge/status-live-brightgreen?style=flat-square" />
  <img src="https://img.shields.io/badge/license-MIT-blue?style=flat-square" />
</p>

<h1 align="center">Aurum — Personal Finance API</h1>

<p align="center">
  API RESTful para gestão de finanças pessoais. Gerencie receitas, despesas e categorias por usuário,
  com endpoints otimizados para dashboards financeiros — resumo mensal, exportação CSV e busca avançada com filtros combinados.
</p>

<p align="center">
  <a href="https://aurum-finance-api.onrender.com/swagger-ui.html"><strong>Swagger UI →</strong></a>
  &nbsp;·&nbsp;
  <a href="https://aurum-finance-api.onrender.com/actuator/health">Health Check →</a>
</p>

---

## Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Modelo de dados](#modelo-de-dados)
- [Endpoints](#endpoints)
- [Segurança](#segurança)
- [Cache e performance](#cache-e-performance)
- [Como executar](#como-executar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Testes](#testes)
- [Decisões técnicas](#decisões-técnicas)
- [Roadmap](#roadmap)
- [Autor](#autor)

---

## Tecnologias

| Camada | Tecnologia | Versão |
|---|---|---|
| Linguagem | Java | 21 |
| Framework | Spring Boot | 4.0.5 |
| Banco de dados | PostgreSQL | 17 |
| Migrations | Flyway | — |
| ORM | Spring Data JPA + Hibernate | 7.x |
| Segurança | Spring Security + JWT | Auth0 java-jwt 4.4 |
| Cache | Spring Cache + Caffeine | — |
| Rate limiting | Bucket4j | 8.10 |
| Validação | Jakarta Bean Validation | — |
| Documentação | SpringDoc OpenAPI 3 (Swagger UI) | 3.0.2 |
| Monitoramento | Spring Boot Actuator | — |
| Build | Maven | 3.9+ |
| Testes | JUnit 5 + Mockito + MockMvc | — |
| Utilitários | Lombok | — |

---

## Arquitetura

O projeto segue arquitetura em camadas padrão Spring Boot, com separação clara de responsabilidades:

```
┌──────────────────────────────────────────────────────────────┐
│                        HTTP Request                          │
│                    (+ JWT + Rate Limiter)                    │
└───────────────────────────┬──────────────────────────────────┘
                            │
                    ┌───────▼────────┐
                    │  Controllers   │  AuthController, TransactionController
                    │                │  CategoryController, UserController
                    └───────┬────────┘
                            │
              ┌─────────────▼──────────────┐
              │         Services           │  AuthService, TransactionService
              │                            │  CategoryService, TokenService
              │   ┌────────────────────┐   │
              │   │ TransactionCache   │   │  Cache isolado (AOP-safe)
              │   │    Service         │   │
              │   └────────────────────┘   │
              └─────────────┬──────────────┘
                            │
                    ┌───────▼────────┐
                    │ Repositories   │  JpaRepository + JpaSpecificationExecutor
                    └───────┬────────┘
                            │
                    ┌───────▼────────┐
                    │  PostgreSQL    │  5 migrations Flyway
                    └────────────────┘
```

### Estrutura de pacotes

```
src/
└── main/
    └── java/com/bentorangel/finance_dashboard/
        ├── config/          # SecurityConfig, CacheConfig, OpenApiConfig,
        │                    # RateLimitInterceptor, WebMvcConfig, SecurityFilter
        ├── controller/      # AuthController, TransactionController,
        │                    # CategoryController, UserController
        ├── dto/             # Records de request/response
        ├── exception/       # GlobalExceptionHandler, BusinessException,
        │                    # ResourceNotFoundException, ErrorResponse
        ├── model/           # User, Transaction, Category, CategoryType
        ├── repository/      # TransactionRepository, CategoryRepository,
        │                    # UserRepository
        └── service/         # AuthService, TransactionService, CategoryService,
                             # TransactionCacheService, AuthorizationService,
                             # TokenService
```

---

## Modelo de dados

### Diagrama de entidades

```
┌─────────────┐       ┌──────────────────┐       ┌─────────────────┐
│    users    │       │   transactions   │       │   categories    │
├─────────────┤       ├──────────────────┤       ├─────────────────┤
│ id (UUID)   │◄──┐   │ id (UUID)        │   ┌──►│ id (UUID)       │
│ name        │   │   │ description      │   │   │ name            │
│ email       │   │   │ amount           │   │   │ type (ENUM)     │
│ password    │   │   │ transaction_date │   │   │ user_id (FK)    │
│ created_at  │   │   │ category_id (FK) │───┘   │ created_at      │
│ updated_at  │   │   │ user_id (FK)     │───┐   │ updated_at      │
│ active      │   └───│                  │   └──►│ active          │
│ version     │       │ created_at       │       │ version         │
└─────────────┘       │ updated_at       │       └─────────────────┘
                      │ active           │
                      │ version          │
                      └──────────────────┘
```

### Migrations Flyway

| Versão | Arquivo | Descrição |
|---|---|---|
| V1 | `V1__init_schema.sql` | Schema inicial — tabelas `categories` e `transactions` |
| V2 | `V2__add_auditing_and_soft_delete.sql` | Auditoria (`updated_at`) e soft delete (`active`) |
| V3 | `V3__add_performance_indexes.sql` | Índices em `transaction_date`, `category_id`, `active` |
| V4 | `V4__add_optimistic_locking.sql` | Coluna `version` para optimistic locking |
| V5 | `V5__add_users_and_security.sql` | Tabela `users`, FKs e índices de isolamento multi-tenant |

---

## Endpoints

**Base URL:** `https://aurum-finance-api.onrender.com/api/v1`

> Documentação interativa completa: [`/swagger-ui.html`](https://aurum-finance-api.onrender.com/swagger-ui.html)

### Auth — público

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| `POST` | `/auth/register` | Registra novo usuário | `201 Created` |
| `POST` | `/auth/login` | Autentica e retorna token JWT | `200 OK` |

### Users — requer JWT

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| `GET` | `/users/me` | Retorna dados do usuário autenticado | `200 OK` |

### Categories — requer JWT

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| `POST` | `/categories` | Cria categoria | `201 Created` |
| `GET` | `/categories` | Lista categorias paginadas | `200 OK` |
| `GET` | `/categories/{id}` | Busca categoria por ID (cacheado) | `200 OK` |
| `PUT` | `/categories/{id}` | Atualiza categoria | `200 OK` |
| `DELETE` | `/categories/{id}` | Remove categoria (soft delete) | `204 No Content` |

### Transactions — requer JWT

| Método | Endpoint | Descrição | Status |
|---|---|---|---|
| `POST` | `/transactions` | Cria transação | `201 Created` |
| `GET` | `/transactions` | Lista transações paginadas | `200 OK` |
| `GET` | `/transactions/{id}` | Busca transação por ID | `200 OK` |
| `PUT` | `/transactions/{id}` | Atualiza transação | `200 OK` |
| `DELETE` | `/transactions/{id}` | Remove transação (soft delete) | `204 No Content` |
| `GET` | `/transactions/period` | Filtra por período | `200 OK` |
| `GET` | `/transactions/search` | Busca avançada com filtros combinados | `200 OK` |
| `GET` | `/transactions/summary` | Resumo financeiro do período (cacheado) | `200 OK` |
| `GET` | `/transactions/summary/monthly` | Resumo mensal agrupado para gráficos | `200 OK` |
| `GET` | `/transactions/export` | Exporta CSV UTF-8 do período | `200 OK` |

### Parâmetros de query

**`GET /transactions/search`**
| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `description` | `string` | Não | Filtro por descrição (like, case-insensitive) |
| `categoryId` | `UUID` | Não | Filtro por categoria |
| `type` | `INCOME\|EXPENSE` | Não | Filtro por tipo |
| `startDate` | `YYYY-MM-DD` | Não | Data inicial do período |
| `endDate` | `YYYY-MM-DD` | Não | Data final do período |
| `page` | `int` | Não | Número da página (padrão: 0) |
| `size` | `int` | Não | Itens por página (padrão: 20) |

**`GET /transactions/summary/monthly`**
| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `year` | `int` | Não | Ano de referência (padrão: ano atual) |

### Exemplos de request

**Registro:**
```json
POST /api/v1/auth/register
Content-Type: application/json

{
  "name": "Bento Rangel",
  "email": "bento@email.com",
  "password": "minhasenha123"
}
```

**Login:**
```json
POST /api/v1/auth/login
Content-Type: application/json

{
  "email": "bento@email.com",
  "password": "minhasenha123"
}
```

**Criar transação:**
```json
POST /api/v1/transactions
Authorization: Bearer <token>
Content-Type: application/json

{
  "description": "Salário de Abril",
  "amount": 5000.00,
  "transactionDate": "2026-04-05",
  "categoryId": "3fa85f64-5717-4562-b3fc-2c963f66afa6"
}
```

**Busca avançada:**
```
GET /api/v1/transactions/search?type=EXPENSE&startDate=2026-01-01&endDate=2026-03-31&page=0&size=10
Authorization: Bearer <token>
```

### Respostas de erro

Todos os erros seguem o padrão:

```json
{
  "timestamp": "2026-04-05T10:30:00",
  "status": 404,
  "error": "Not Found",
  "message": "Transação não encontrada ou não pertence a você.",
  "path": "/api/v1/transactions/uuid-inexistente"
}
```

| Status | Situação |
|---|---|
| `400 Bad Request` | Validação falhou (`@NotBlank`, `@NotNull`, etc.) |
| `401 Unauthorized` | Token ausente, inválido ou expirado |
| `404 Not Found` | Recurso não encontrado |
| `409 Conflict` | Regra de negócio violada ou conflito de concorrência |
| `429 Too Many Requests` | Rate limit excedido (20 req/min por IP) |
| `500 Internal Server Error` | Erro inesperado |

---

## Segurança

### Autenticação JWT

Toda requisição autenticada deve incluir o header:
```
Authorization: Bearer <token>
```

O token é validado em cada requisição pelo `SecurityFilter`, que carrega o usuário no `SecurityContextHolder`. Validade: **2 horas**.

### Isolamento de dados

Todas as queries filtram pelo usuário autenticado. Um usuário não consegue acessar, modificar ou deletar dados de outro — mesmo conhecendo o UUID do recurso.

### Rate limiting

Implementado via Bucket4j + Caffeine. Limite de **20 requisições por minuto por IP**. Entradas inativas por mais de 1 hora são removidas automaticamente. Retorna `429 Too Many Requests` quando o limite é atingido.

Pode ser desabilitado via propriedade (usado em testes de integração):
```properties
app.security.rate-limit.enabled=false
```

### Optimistic locking

Todas as entidades possuem campo `version` (`@Version`). Atualizações concorrentes na mesma entidade retornam `409 Conflict` em vez de silenciosamente sobrescrever dados.

### Soft delete

Entidades nunca são removidas fisicamente. `@SQLDelete` converte `delete()` em `UPDATE active = false`, e `@SQLRestriction` garante que registros inativos não aparecem em nenhuma query JPQL.

---

## Cache e performance

### Caffeine Cache

Configurado com TTL de **60 minutos** e máximo de **1.000 itens**.

| Cache | Chave | Invalidado em |
|---|---|---|
| `dashboardSummary` | `email-startDate-endDate` | `create`, `update`, `delete` de transação |
| `categoria` | `id-email` | `update`, `delete` da categoria |

O `TransactionCacheService` isola a lógica de `@Cacheable` do `TransactionService`, evitando o problema de self-invocation do Spring AOP.

### Índices de banco

```sql
idx_transactions_date        -- filtros por data
idx_transactions_category_id -- agrupamentos por categoria
idx_transactions_active      -- exclusão de soft-deleted
idx_categories_active        -- exclusão de soft-deleted
idx_categories_user_id       -- isolamento multi-tenant
idx_transactions_user_id     -- isolamento multi-tenant
```

### JPA Specification + EntityGraph

O endpoint `/search` usa `JpaSpecificationExecutor` para compor predicados dinamicamente. O `@EntityGraph(attributePaths = {"category"})` no `findAll(Specification, Pageable)` carrega a categoria em um único JOIN no SELECT de dados, sem gerar JOIN desnecessário na query de contagem da paginação.

---

## Como executar

### Pré-requisitos

- Java 21+
- Maven 3.9+
- Docker e Docker Compose

### Com Docker (recomendado)

```bash
# 1. Clone o repositório
git clone https://github.com/bentors/aurum-finance-api.git
cd aurum-finance-api

# 2. Configure as variáveis de ambiente
cp .env.example .env
# edite o .env com suas credenciais

# 3. Suba banco + aplicação
docker-compose up -d

# 4. Acesse a documentação
open http://localhost:8080/swagger-ui.html
```

O Flyway criará todas as tabelas automaticamente na primeira inicialização.

### Apenas a aplicação (banco externo)

```bash
# Configure as variáveis de ambiente (ver seção abaixo)
export DB_URL=jdbc:postgresql://localhost:5432/financas_db
export DB_USER=postgres
export DB_PASSWORD=sua_senha
export API_SECURITY_TOKEN_SECRET=sua-chave-jwt-secreta

# Execute
./mvnw spring-boot:run
```

### Perfil de desenvolvimento

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

O perfil `dev` habilita `show-sql=true`, `format_sql=true` e expõe métricas completas no Actuator.

---

## Variáveis de ambiente

| Variável | Descrição | Obrigatória |
|---|---|---|
| `DB_URL` | URL JDBC do PostgreSQL (`jdbc:postgresql://host:5432/db`) | ✅ |
| `DB_USER` | Usuário do banco | ✅ |
| `DB_PASSWORD` | Senha do banco | ✅ |
| `API_SECURITY_TOKEN_SECRET` | Chave secreta para assinar tokens JWT (mínimo 32 chars) | ✅ |
| `PORT` | Porta da aplicação (padrão: `8080`) | ❌ |
| `APP_SECURITY_RATE_LIMIT_ENABLED` | Habilita rate limiting (padrão: `true`) | ❌ |
| `APP_CORS_ALLOWED_ORIGINS` | Origins permitidas pelo CORS | ❌ |

> A aplicação **não inicializa** se qualquer variável obrigatória estiver ausente.

Crie um arquivo `.env` na raiz do projeto:
```env
DB_URL=jdbc:postgresql://localhost:5454/financas_db
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui
API_SECURITY_TOKEN_SECRET=sua-chave-jwt-super-secreta-minimo-32-chars
```

---

## Testes

### Cobertura

| Arquivo | Tipo | Cenários |
|---|---|---|
| `TransactionServiceTest` | Unitário | 20 (CRUD, CSV, cache, busca avançada, períodos, ownership) |
| `CategoryServiceTest` | Unitário | 9 (CRUD, duplicata, ownership, cache evict) |
| `AuthServiceTest` | Unitário | 4 (login, bad credentials, register, e-mail duplicado) |
| `AuthorizationServiceTest` | Unitário | 2 (loadUserByUsername — sucesso e não encontrado) |
| `TokenServiceTest` | Unitário | 4 (geração, validação, expirado, inválido) |
| `TransactionControllerIntegrationTest` | Integração | ~40 (fluxo HTTP completo com MockMvc) |
| `CategoryControllerIntegrationTest` | Integração | 5 (CRUD via HTTP) |
| `AuthControllerIntegrationTest` | Integração | 6 (login e registro via HTTP) |
| `UserControllerIntegrationTest` | Integração | 2 (GET /me) |

### Executar

```bash
# Todos os testes
./mvnw test

# Apenas unitários
./mvnw test -Dtest="*ServiceTest,*TokenServiceTest"

# Apenas integração
./mvnw test -Dtest="*IntegrationTest"
```

> Os testes de integração usam `application-test.properties` com rate limit desabilitado (`app.security.rate-limit.enabled=false`).

---

## Decisões técnicas

### JpaSpecificationExecutor no `/search`

O endpoint de busca tem 5 filtros opcionais combináveis. Uma query JPQL com condicionais manuais seria propensa a erros e difícil de manter. `Specification` compõe os predicados de forma segura, tipada e extensível. O `@EntityGraph` no `findAll(Specification, Pageable)` resolve o carregamento da categoria sem gerar JOIN no `COUNT(*)` da paginação.

### TransactionCacheService separado

O Spring AOP intercepta chamadas externas ao bean — chamadas internas (`this.método()`) bypassam o proxy e `@Cacheable` não executa. Extrair a lógica cacheável para um bean separado resolve o problema de self-invocation, evita `@Lazy` e torna o cache completamente testável de forma isolada.

### Soft delete com @SQLDelete + @SQLRestriction

Dados financeiros históricos têm valor e não devem ser destruídos permanentemente. O `@SQLDelete` converte `delete()` em `UPDATE active = false` automaticamente — o código de negócio não precisa saber que é soft delete. O `@SQLRestriction` garante que registros inativos nunca aparecem em queries JPQL, sem precisar adicionar `AND active = true` manualmente em nenhuma query.

### Caffeine em vez de Redis

Para portfólio e desenvolvimento local, Caffeine oferece cache in-process com TTL configurável sem dependência de infraestrutura adicional. A interface `CacheManager` do Spring é a mesma — migrar para Redis em produção exige apenas trocar a implementação no `CacheConfig`, sem alterar nenhum service.

### Optimistic locking

Todas as entidades têm `@Version`. A coluna `version` é incrementada a cada update — se dois requests tentarem atualizar a mesma entidade simultaneamente, o segundo recebe `OptimisticLockingFailureException`, que o `GlobalExceptionHandler` captura e retorna como `409 Conflict` com mensagem legível.

### native query no getMonthlySummary

O agrupamento por mês com SUM condicional por tipo (`INCOME`/`EXPENSE`) é mais expressivo em SQL nativo do que em JPQL. O `@SQLRestriction` do Hibernate não se aplica a native queries, então os filtros `AND t.active = true AND c.active = true` estão explícitos na query para manter a consistência com o soft delete.

---

## Roadmap

- [ ] Refresh token (renovação de sessão sem novo login)
- [ ] Budget por categoria (metas mensais de gasto com alertas)
- [ ] Transações recorrentes com job `@Scheduled`
- [ ] Endpoint `/summary/by-category` para gráfico de pizza
- [ ] Microserviço Python / FastAPI para analytics avançados
- [ ] Spring Profiles explícitos (`dev` / `prod`) no `docker-compose`
- [ ] Frontend React + TypeScript + Recharts (em desenvolvimento)

---

## Autor

**Bento Rangel**

Software Engineer.

[![Portfolio](https://img.shields.io/badge/Portfolio-bentorangel.vercel.app-6C63FF?style=flat-square)](https://bentorangel.vercel.app)
[![GitHub](https://img.shields.io/badge/GitHub-bentors-181717?style=flat-square&logo=github)](https://github.com/bentors)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Bento%20Rangel-0A66C2?style=flat-square&logo=linkedin)](https://www.linkedin.com/in/bento-rangel)

---

<p align="center">Desenvolvido com Java e Spring Boot · Aurum Personal Finance</p>