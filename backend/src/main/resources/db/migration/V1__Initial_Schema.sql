-- V1__Initial_Schema.sql
-- Estructura limpia sin datos corruptos
-- 1. Crear Tablas
CREATE TABLE public.course_students (
    user_id uuid NOT NULL,
    course_id uuid NOT NULL
);
CREATE TABLE public.courses (
    id uuid NOT NULL,
    active boolean NOT NULL,
    created_at timestamp(6) without time zone,
    description text,
    hours integer NOT NULL,
    price numeric(38, 2) NOT NULL,
    title character varying(255) NOT NULL,
    updated_at timestamp(6) without time zone,
    teacher_id uuid NOT NULL,
    cover_image character varying(255)
);
CREATE TABLE public.enrollments (
    id uuid NOT NULL,
    amount_paid numeric(38, 2),
    enrolled_at timestamp(6) without time zone,
    payment_id character varying(255),
    course_id uuid NOT NULL,
    user_id uuid NOT NULL
);
CREATE TABLE public.lesson_progress (
    id uuid NOT NULL,
    completed_at timestamp(6) without time zone,
    is_completed boolean NOT NULL,
    lesson_id uuid NOT NULL,
    user_id uuid NOT NULL
);
CREATE TABLE public.lessons (
    id uuid NOT NULL,
    order_index integer NOT NULL,
    pdf_url character varying(255),
    title character varying(255) NOT NULL,
    video_url character varying(255),
    module_id uuid NOT NULL,
    duration integer
);
CREATE TABLE public.modules (
    id uuid NOT NULL,
    order_index integer NOT NULL,
    title character varying(255) NOT NULL,
    course_id uuid NOT NULL
);
CREATE TABLE public.password_reset_token (
    id bigint NOT NULL,
    expiry_date timestamp(6) without time zone,
    token character varying(255),
    user_id uuid NOT NULL
);
CREATE TABLE public.users (
    id uuid NOT NULL,
    birth_date date,
    created_at timestamp(6) without time zone,
    dni character varying(9) NOT NULL,
    email character varying(255) NOT NULL,
    name character varying(255) NOT NULL,
    password character varying(255) NOT NULL,
    phone character varying(9),
    region character varying(255) NOT NULL,
    role character varying(255) NOT NULL,
    surname character varying(255) NOT NULL,
    terms_accepted boolean NOT NULL,
    avatar character varying(255),
    CONSTRAINT users_role_check CHECK (
        (
            (role)::text = ANY (
                (
                    ARRAY ['ADMIN'::character varying, 'STUDENT'::character varying, 'TEACHER'::character varying]
                )::text []
            )
        )
    )
);
-- 2. Secuencias
CREATE SEQUENCE public.password_reset_token_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
ALTER SEQUENCE public.password_reset_token_id_seq OWNED BY public.password_reset_token.id;
ALTER TABLE ONLY public.password_reset_token
ALTER COLUMN id
SET DEFAULT nextval('public.password_reset_token_id_seq'::regclass);
-- 3. Primary Keys
ALTER TABLE ONLY public.course_students
ADD CONSTRAINT course_students_pkey PRIMARY KEY (user_id, course_id);
ALTER TABLE ONLY public.courses
ADD CONSTRAINT courses_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.enrollments
ADD CONSTRAINT enrollments_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.lesson_progress
ADD CONSTRAINT lesson_progress_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.lessons
ADD CONSTRAINT lessons_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.modules
ADD CONSTRAINT modules_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.password_reset_token
ADD CONSTRAINT password_reset_token_pkey PRIMARY KEY (id);
ALTER TABLE ONLY public.users
ADD CONSTRAINT users_pkey PRIMARY KEY (id);
-- 4. Constraints y Foreign Keys
ALTER TABLE ONLY public.users
ADD CONSTRAINT uk6dotkott2kjsp8vw4d0m25fb7 UNIQUE (email);
ALTER TABLE ONLY public.lesson_progress
ADD CONSTRAINT uk7lok1iwll7jsobapv1rmr563 UNIQUE (user_id, lesson_id);
ALTER TABLE ONLY public.password_reset_token
ADD CONSTRAINT uk_f90ivichjaokvmovxpnlm5nin UNIQUE (user_id);
ALTER TABLE ONLY public.enrollments
ADD CONSTRAINT fk3hjx6rcnbmfw368sxigrpfpx0 FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.password_reset_token
ADD CONSTRAINT fk83nsrttkwkb6ym0anu051mtxn FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.modules
ADD CONSTRAINT fk8qnnp812q1jd38fx7mxrhpw9 FOREIGN KEY (course_id) REFERENCES public.courses(id);
ALTER TABLE ONLY public.enrollments
ADD CONSTRAINT fkho8mcicp4196ebpltdn9wl6co FOREIGN KEY (course_id) REFERENCES public.courses(id);
ALTER TABLE ONLY public.lesson_progress
ADD CONSTRAINT fkhxwj6gbacmwi2768sceg602uf FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.course_students
ADD CONSTRAINT fkj5fbpmgy0y0es0gvk0311jor3 FOREIGN KEY (course_id) REFERENCES public.courses(id);
ALTER TABLE ONLY public.course_students
ADD CONSTRAINT fkq1158o0xpbhoxtihu2w854c8j FOREIGN KEY (user_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.lesson_progress
ADD CONSTRAINT fkqwr70bkn0j6gok1y4op9jns8y FOREIGN KEY (lesson_id) REFERENCES public.lessons(id);
ALTER TABLE ONLY public.courses
ADD CONSTRAINT fkt4ba5fab1x56tmt4nsypv5lm5 FOREIGN KEY (teacher_id) REFERENCES public.users(id);
ALTER TABLE ONLY public.lessons
ADD CONSTRAINT fkt9yjhjbd9y3w6fxs66ny1wu02 FOREIGN KEY (module_id) REFERENCES public.modules(id);