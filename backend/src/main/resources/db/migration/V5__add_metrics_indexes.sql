-- USERS: registros por tiempo
CREATE INDEX IF NOT EXISTS idx_users_created_at ON users (created_at);
-- ENROLLMENTS: matrículas e ingresos por tiempo
CREATE INDEX IF NOT EXISTS idx_enrollments_enrolled_at ON enrollments (enrolled_at);
CREATE INDEX IF NOT EXISTS idx_enrollments_course_id ON enrollments (course_id);
CREATE INDEX IF NOT EXISTS idx_enrollments_user_id ON enrollments (user_id);
-- Si haces búsquedas/dedupe por Stripe
CREATE INDEX IF NOT EXISTS idx_enrollments_payment_id ON enrollments (payment_id);