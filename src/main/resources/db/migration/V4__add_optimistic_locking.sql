-- Evolução: Adicionando controle de concorrência (Optimistic Locking)

ALTER TABLE categories ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;
ALTER TABLE transactions ADD COLUMN version BIGINT DEFAULT 0 NOT NULL;