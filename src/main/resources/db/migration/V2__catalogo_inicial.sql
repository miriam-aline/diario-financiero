-- Catálogo base genérico (sin datos reales) para poder usar la aplicación desde el primer arranque
INSERT INTO categoria (nombre, presupuesto_mensual) VALUES
    ('Comida', NULL),
    ('Transporte', NULL),
    ('Vivienda', NULL),
    ('Servicios', NULL),
    ('Salud', NULL),
    ('Entretenimiento', NULL),
    ('Compras', NULL),
    ('Salario', NULL),
    ('Otros ingresos', NULL),
    ('Sin categoría', NULL);

INSERT INTO cuenta (nombre, tipo, dia_corte, dia_pago) VALUES
    ('Efectivo', 'EFECTIVO', NULL, NULL),
    ('Débito principal', 'DEBITO', NULL, NULL);
