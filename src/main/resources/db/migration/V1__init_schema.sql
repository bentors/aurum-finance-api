-- Criando a tabela de Categorias
CREATE TABLE categories (
                            id UUID PRIMARY KEY,
                            name VARCHAR(100) NOT NULL,
                            type VARCHAR(20) NOT NULL CHECK (type IN ('INCOME', 'EXPENSE')),
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Criando a tabela de Transações
CREATE TABLE transactions (
                              id UUID PRIMARY KEY,
                              description VARCHAR(255) NOT NULL,
                              amount DECIMAL(19, 4) NOT NULL, -- Precisão ideal para dinheiro
                              transaction_date DATE NOT NULL,
                              category_id UUID NOT NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_category FOREIGN KEY (category_id) REFERENCES categories(id)
);