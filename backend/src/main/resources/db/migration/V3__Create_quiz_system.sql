-- 1. Crear tabla de Exámenes (Quizzes)
CREATE TABLE public.quizzes (
    id uuid NOT NULL,
    title character varying(255),
    lesson_id uuid,
    -- Relación 1 a 1 con lección
    CONSTRAINT quizzes_pkey PRIMARY KEY (id),
    CONSTRAINT fk_quiz_lesson FOREIGN KEY (lesson_id) REFERENCES public.lessons(id)
);
-- 2. Crear tabla de Preguntas
CREATE TABLE public.questions (
    id uuid NOT NULL,
    content text NOT NULL,
    points integer,
    quiz_id uuid NOT NULL,
    CONSTRAINT questions_pkey PRIMARY KEY (id),
    CONSTRAINT fk_question_quiz FOREIGN KEY (quiz_id) REFERENCES public.quizzes(id) ON DELETE CASCADE
);
-- 3. Crear tabla de Opciones (Respuestas)
CREATE TABLE public.options (
    id uuid NOT NULL,
    is_correct boolean NOT NULL,
    text character varying(255) NOT NULL,
    question_id uuid NOT NULL,
    CONSTRAINT options_pkey PRIMARY KEY (id),
    CONSTRAINT fk_option_question FOREIGN KEY (question_id) REFERENCES public.questions(id) ON DELETE CASCADE
);