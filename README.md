# Finance Dashboard API

API RESTful para gestão de finanças pessoais, desenvolvida com Java 21 e Spring Boot 4. Permite gerenciar receitas, despesas e categorias por usuário, com endpoints otimizados para alimentar um dashboard financeiro — resumo mensal, exportação CSV e busca avançada com filtros combinados.

---

## Sumário

- [Tecnologias](#tecnologias)
- [Arquitetura](#arquitetura)
- [Funcionalidades](#funcionalidades)
- [Endpoints](#endpoints)
- [Segurança](#segurança)
- [Como executar](#como-executar)
- [Variáveis de ambiente](#variáveis-de-ambiente)
- [Testes](#testes)
- [Decisões técnicas](#decisões-técnicas)
- [Roadmap](#roadmap)
- [Autor](#autor)

---

## Tecnologias

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 21 |
| Framework | Spring Boot 4.0.5 |
| Banco de dados | PostgreSQL 18 |
| Migrations | Flyway |
| ORM | Spring Data JPA + Hibernate |
| Segurança | Spring Security + JWT (Auth0 java-jwt 4.4) |
| Cache | Spring Cache + Caffeine |
| Rate limiting | Bucket4j 8.10 + Caffeine |
| Validação | Jakarta Bean Validation |
| Documentação | SpringDoc OpenAPI 3 (Swagger UI) |
| Monitoramento | Spring Boot Actuator |
| Build | Maven |
| Testes | JUnit 5 + Mockito + MockMvc |
| Utilitários | Lombok |

---

## Arquitetura

O projeto segue a arquitetura em camadas padrão do Spring Boot, com separação clara de responsabilidades:

```
Controller  →  Service  →  Repository  →  PostgreSQL
                ↕
         CacheService
         (Caffeine / Spring Cache)
```

```
src/
└── main/
    └── java/com/bentorangel/finance_dashboard/
        ├── config/          # Segurança, CORS, Cache, Rate Limit
        ├── controller/      # Camada HTTP — AuthController, TransactionController,
        │                    # CategoryController, UserController
        ├── dto/             # Records de request/response
        ├── exception/       # GlobalExceptionHandler + exceções customizadas
        ├── model/           # Entidades JPA (User, Transaction, Category)
        ├── repository/      # Interfaces Spring Data JPA
        └── service/         # Regras de negócio — AuthService, TransactionService,
                             # CategoryService, TransactionCacheService, TokenService
```

### Decisões de design

**Multi-tenancy por usuário** — todas as queries são filtradas pelo usuário autenticado. Não existe endpoint que retorne dados de outro usuário, independente de qualquer parâmetro.

**Soft delete** — entidades nunca são removidas fisicamente do banco. A flag `active = false` é setada pelo Hibernate via `@SQLDelete`, e `@SQLRestriction` garante que registros inativos nunca apareçam em queries JPQL.

**Optimistic locking** — todas as entidades possuem campo `version` (`@Version`), prevenindo atualizações concorrentes silenciosas.

**Cache separado em serviço dedicado** — o `TransactionCacheService` isola a lógica de cache (`@Cacheable` / `@CacheEvict`) do `TransactionService`, evitando o problema de self-invocation do Spring AOP e tornando o cache completamente testável de forma isolada.

---

## Funcionalidades

### Autenticação
- Registro de novo usuário com validação de e-mail único
- Login com geração de token JWT (validade: 2 horas)
- Perfil do usuário autenticado (`/me`)

### Categorias
- CRUD completo de categorias (INCOME / EXPENSE)
- Validação de nome duplicado por usuário (case-insensitive)
- Cache por ID com invalidação automática em update e delete

### Transações
- CRUD completo com paginação e ordenação
- Filtro por período (`/period`)
- Busca avançada com filtros combinados opcionais: descrição (like), categoria, tipo e intervalo de datas (`/search`)
- Resumo financeiro do período: total de receitas, despesas e saldo (`/summary`)
- Resumo mensal agrupado por mês para gráficos de linha (`/summary/monthly`)
- Exportação para CSV em UTF-8 (`/export`)

---

## Endpoints

Base URL: `http://localhost:8081/api/v1`

> Documentação interativa completa disponível em `http://localhost:8081/swagger-ui.html`

### Auth

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/auth/register` | Público | Registra novo usuário |
| POST | `/auth/login` | Público | Retorna token JWT |

### Users

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| GET | `/users/me` | JWT | Retorna dados do usuário logado |

### Categories

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/categories` | JWT | Cria categoria |
| GET | `/categories` | JWT | Lista categorias paginadas |
| GET | `/categories/{id}` | JWT | Busca categoria por ID |
| PUT | `/categories/{id}` | JWT | Atualiza categoria |
| DELETE | `/categories/{id}` | JWT | Remove categoria (soft delete) |

### Transactions

| Método | Endpoint | Auth | Descrição |
|---|---|---|---|
| POST | `/transactions` | JWT | Cria transação |
| GET | `/transactions` | JWT | Lista transações paginadas |
| GET | `/transactions/{id}` | JWT | Busca transação por ID |
| PUT | `/transactions/{id}` | JWT | Atualiza transação |
| DELETE | `/transactions/{id}` | JWT | Remove transação (soft delete) |
| GET | `/transactions/period` | JWT | Filtra por período (`startDate`, `endDate`) |
| GET | `/transactions/search` | JWT | Busca avançada (descrição, categoria, tipo, período) |
| GET | `/transactions/summary` | JWT | Resumo financeiro do período |
| GET | `/transactions/summary/monthly` | JWT | Resumo mensal por ano (`year`) |
| GET | `/transactions/export` | JWT | Exporta CSV do período |

### Exemplos de request

**Login:**
```json
POST /api/v1/auth/login
{
  "email": "usuario@email.com",
  "password": "minhasenha123"
}
```

**Criar transação:**
```json
POST /api/v1/transactions
Authorization: Bearer <token>

{
  "description": "Salário de Abril",
  "amount": 5000.00,
  "transactionDate": "2026-04-05",
  "categoryId": "uuid-da-categoria"
}
```

**Busca avançada:**
```
GET /api/v1/transactions/search?type=EXPENSE&startDate=2026-01-01&endDate=2026-03-31&page=0&size=10
Authorization: Bearer <token>
```

**Resumo mensal:**
```
GET /api/v1/transactions/summary/monthly?year=2026
Authorization: Bearer <token>
```

---

## Segurança

### Autenticação JWT
Toda requisição autenticada deve incluir o header:
```
Authorization: Bearer <token>
```

O token é validado em cada requisição pelo `SecurityFilter`, que carrega o usuário no `SecurityContextHolder`. Tokens inválidos, expirados ou adulterados retornam `401 Unauthorized`.

### Rate Limiting
Implementado via Bucket4j com cache Caffeine. Limite de **20 requisições por minuto por IP**. IPs inativos por mais de 1 hora são removidos automaticamente do cache. Retorna `429 Too Many Requests` quando o limite é atingido.

O rate limit pode ser desabilitado via propriedade (útil para testes):
```properties
app.security.rate-limit.enabled=false
```

### Isolamento de dados
Todas as queries verificam o usuário autenticado. Um usuário não consegue acessar, modificar ou deletar dados de outro usuário — mesmo conhecendo o UUID do recurso.

---

## Como executar

### Pré-requisitos
- Java 21
- Maven 3.9+
- Docker e Docker Compose

### Com Docker (recomendado)

1. Clone o repositório:
```bash
git clone https://github.com/bentors/finance-dashboard.git
cd finance-dashboard
```

2. Configure as variáveis de ambiente (crie um arquivo `.env` na raiz):
```env
DB_URL=jdbc:postgresql://localhost:5454/financas_db
DB_USER=postgres
DB_PASSWORD=sua_senha_aqui
API_SECURITY_TOKEN_SECRET=sua-chave-jwt-super-secreta-aqui
```

3. Suba o banco de dados:
```bash
docker-compose up -d
```

4. Execute a aplicação:
```bash
./mvnw spring-boot:run
```

O Flyway criará todas as tabelas automaticamente na primeira inicialização.

5. Acesse a documentação:
```
http://localhost:8081/swagger-ui.html
```

### Sem Docker

Configure uma instância PostgreSQL local e ajuste `DB_URL` para apontar para ela. O resto dos passos é idêntico.

---

## Variáveis de ambiente

| Variável | Descrição | Obrigatória |
|---|---|---|
| `DB_URL` | URL JDBC do PostgreSQL | Sim |
| `DB_USER` | Usuário do banco | Sim |
| `DB_PASSWORD` | Senha do banco | Sim |
| `API_SECURITY_TOKEN_SECRET` | Chave secreta para assinar tokens JWT | Sim |
| `APP_SECURITY_RATE_LIMIT_ENABLED` | Habilita/desabilita o rate limiting (padrão: `true`) | Não |

> A aplicação **não inicializa** sem as quatro variáveis obrigatórias configuradas.

---

## Testes

O projeto possui três camadas de testes:

**Testes unitários de service** — cobrem a lógica de negócio de forma isolada usando Mockito. Incluem cenários de sucesso, exceções esperadas, proteção de ownership e comportamento null-safe.

**Testes de integração de controller** — usam MockMvc para testar o fluxo HTTP completo, incluindo autenticação, validações de request e respostas de erro.

**Cobertura atual:**
- `TransactionService` — 14 cenários (CRUD, CSV, cache, busca, períodos)
- `CategoryService` — criação, duplicata, ownership, update, delete
- `AuthorizationService` — loadUserByUsername com usuário existente e inexistente
- `TokenService` — geração e validação de token
- `AuthController` — login e registro (integração)
- `CategoryController` — CRUD completo (integração)
- `UserController` — GET /me (integração)

Para executar:
```bash
./mvnw test
```

---

## Decisões técnicas

### Por que `JpaSpecificationExecutor` no `/search`?
A busca avançada possui múltiplos filtros opcionais. A alternativa seria uma query JPQL com condicionais manuais, que é propensa a erros e difícil de manter. `Specification` permite compor predicados de forma segura, tipada e extensível. O `@EntityGraph` no método `findAll(Specification, Pageable)` resolve o carregamento da categoria sem gerar N+1 na query de contagem da paginação.

### Por que `TransactionCacheService` separado?
O Spring AOP intercepta chamadas externas ao bean — chamadas internas (`this.método()`) bypassam o proxy e o `@Cacheable` não executa. Extrair a lógica cacheável para um bean separado é a solução mais limpa, evita a injeção circular com `@Lazy`, e torna o cache testável de forma isolada.

### Por que soft delete?
Dados financeiros históricos têm valor e não devem ser destruídos permanentemente. O soft delete com `@SQLDelete` + `@SQLRestriction` é transparente para a aplicação — o código escreve `delete()` e o Hibernate executa um `UPDATE active = false` automaticamente.

### Por que Caffeine e não Redis?
Para um portfólio e desenvolvimento local, Caffeine oferece cache in-process com TTL configurável sem dependência de infraestrutura adicional. A interface `CacheManager` do Spring é a mesma — migrar para Redis em produção exigiria apenas trocar a implementação no `CacheConfig`, sem alterar uma linha de código nos services.

### Migrations Flyway
O schema é versionado em 5 migrations evolutivas, refletindo a evolução real do projeto:

| Versão | Descrição |
|---|---|
| V1 | Schema inicial — categories e transactions |
| V2 | Auditoria e soft delete |
| V3 | Índices de performance |
| V4 | Optimistic locking (`version`) |
| V5 | Tabela de usuários e associação multi-tenant |

---

## Roadmap

- [ ] Refresh token (renovação de sessão sem novo login)
- [ ] Budget por categoria (metas mensais de gasto)
- [ ] Transações recorrentes com job automático
- [ ] Endpoint `/summary/by-category` para gráfico de pizza
- [ ] Microserviço Python / FastAPI para analytics avançados
- [ ] Handler para `OptimisticLockingFailureException` (409 Conflict)
- [ ] Proteção dos endpoints do Actuator em produção
- [ ] Spring Profiles explícitos (dev / prod)
- [ ] Testes unitários do `AuthService`

---

## Autor

**Bento Rangel**

- Portfólio: [bentorangel.vercel.app](https://bentorangel.vercel.app)
- GitHub: [@bentors](https://github.com/bentors)
- LinkedIn: [Bento Rangel](https://www.linkedin.com/in/bento-rangel)
- Email: bento.rangel05@gmail.com

---

*Desenvolvido com Java e Spring Boot por Bento Rangel*