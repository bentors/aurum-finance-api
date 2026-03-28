-- Evolução: Criando a tabela de Usuários e vinculando com Categorias e Transações de forma estrita

CREATE TABLE users (
                       id UUID PRIMARY KEY,
                       name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       created_at TIMESTAMP NOT NULL,
                       updated_at TIMESTAMP,
                       active BOOLEAN DEFAULT TRUE NOT NULL,
                       version BIGINT DEFAULT 0 NOT NULL
);

-- Adicionando a coluna user_id
ALTER TABLE categories ADD COLUMN user_id UUID NOT NULL;
ALTER TABLE transactions ADD COLUMN user_id UUID NOT NULL;

-- Criando as amarras (Foreign Keys)
ALTER TABLE categories ADD CONSTRAINT fk_categories_user FOREIGN KEY (user_id) REFERENCES users(id);
ALTER TABLE transactions ADD CONSTRAINT fk_transactions_user FOREIGN KEY (user_id) REFERENCES users(id);

-- Otimizando as buscas por usuário
CREATE INDEX idx_categories_user_id ON categories(user_id);
CREATE INDEX idx_transactions_user_id ON transactions(user_id);