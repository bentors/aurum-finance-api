-- Evolução: Adicionando índices para otimizar as buscas no Dashboard

-- Acelera os filtros por data (ex: buscar transações de um mês específico)
CREATE INDEX idx_transactions_date ON transactions(transaction_date);

-- Acelera o agrupamento de gastos por categoria (ex: gráfico de pizza)
CREATE INDEX idx_transactions_category_id ON transactions(category_id);

-- Acelera a filtragem ignorando os registros deletados logicamente
CREATE INDEX idx_transactions_active ON transactions(active);
CREATE INDEX idx_categories_active ON categories(active);