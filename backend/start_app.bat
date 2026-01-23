@echo off
echo ==========================================
echo 🚀 INICIANDO RADIO ACADEMY (LOCAL MODE)
echo ==========================================

:: 1. Variables de Entorno (Tus secretos locales)
set DATABASE_URL=jdbc:postgresql://localhost:5432/radio_db
set DB_USER=admin
set DB_PASSWORD=password123
set JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
set RESEND_API_KEY=re_Rrb2VX9A_4JZaS6w6CqKxXeW4roYb828D
set STRIPE_SECRET_KEY=sk_test_51SpDjSRuOaIRKrgJIhfWr0TdWHpJWUaQ579YjGXjmKFPQ6wintpICxnZcPDKdh60Oqn4c3HCSGYYlDJzChTb8Lur009TeN1pbG
set FRONTEND_URL=http://localhost:4200
set ADMIN_EMAIL=admin@local.dev
set ADMIN_PASSWORD=admin

:: 2. Compilar rápido (se salta los tests para ir rápido)
echo 🔨 Compilando proyecto...
call mvn clean package -DskipTests -q

:: 3. Ejecutar el JAR
echo ▶️ Arrancando Servidor...
echo ------------------------------------------
java -jar target/backend-0.0.1-SNAPSHOT.jar
pause