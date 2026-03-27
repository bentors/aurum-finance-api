-- Evolução: Adicionando controle de atualização e exclusão lógica

ALTER TABLE categories
    ADD COLUMN updated_at TIMESTAMP,
ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;

ALTER TABLE transactions
    ADD COLUMN updated_at TIMESTAMP,
ADD COLUMN active BOOLEAN DEFAULT TRUE NOT NULL;