-- pgAdmin: abrir Query Tool sobre la base de mantenimiento "postgres".
-- Ejecutar con Auto-commit activado, fuera de BEGIN/COMMIT.
-- Ejecutar una sola vez. Si "lys" ya existe, no ejecutar este archivo.
-- El propietario será el usuario conectado; usar ese usuario en DB_USERNAME.
CREATE DATABASE lys WITH ENCODING = 'UTF8' TEMPLATE = template0;
