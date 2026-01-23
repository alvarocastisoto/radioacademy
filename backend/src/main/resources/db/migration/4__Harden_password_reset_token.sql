-- Asegura NOT NULL (si tienes filas viejas nulas, primero límpialas o bórralas)
DELETE FROM public.password_reset_token
WHERE token IS NULL
    OR expiry_date IS NULL;
ALTER TABLE public.password_reset_token
ALTER COLUMN token
SET NOT NULL,
    ALTER COLUMN expiry_date
SET NOT NULL;
-- token único (recomendado)
DO $$ BEGIN IF NOT EXISTS (
    SELECT 1
    FROM pg_constraint
    WHERE conname = 'uk_password_reset_token_token'
) THEN
ALTER TABLE public.password_reset_token
ADD CONSTRAINT uk_password_reset_token_token UNIQUE (token);
END IF;
END $$;