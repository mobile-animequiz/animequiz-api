-- =====================================
-- USERS
-- =====================================
INSERT INTO tb_users (username, email, password_hash, date_of_birth, user_role) VALUES ('Naruto Uzumaki', 'naruto@konoha.com', NULL, NULL, 'USER');
INSERT INTO tb_users (username, email, password_hash, date_of_birth, user_role) VALUES ('Sasuke Uchiha', 'sasuke@konoha.com', NULL, NULL, 'USER');
INSERT INTO tb_users (username, email, password_hash, date_of_birth, user_role) VALUES ('Monkey D. Luffy', 'luffy@onepiece.com', NULL, NULL, 'USER');

-- =====================================
-- QUIZZES (com Enums Difficulty e Category)
-- Regra de pontuação: 10 pontos por acerto x peso da dificuldade (FACIL=1, MEDIO=2, DIFICIL=3).
--   Quiz 1 (FACIL, 3 questões): 10 pontos por acerto -> pontuações possíveis: 0, 10, 20, 30
--   Quiz 2 (MEDIO, 3 questões): 20 pontos por acerto -> pontuações possíveis: 0, 20, 40, 60
-- Categoria: os dois quizzes de exemplo são SHONEN (valores aceitos: SHONEN, SEINEN, SHOJO, JOSEI, KODOMO).
-- =====================================
INSERT INTO tb_quizzes (title, description, difficulty, category) VALUES ('Naruto Shippuden Quiz', 'Conhecimento ninja!', 'FACIL', 'SHONEN');
INSERT INTO tb_quizzes (title, description, difficulty, category) VALUES ('One Piece Quiz', 'Os Chapéus de Palha!', 'MEDIO', 'SHONEN');

-- =====================================
-- QUESTIONS (a ordem define os IDs: 1 a 6)
--   Quiz 1: questões 1, 3 e 4   |   Quiz 2: questões 2, 5 e 6
-- =====================================
INSERT INTO tb_questions (statement, quiz_id) VALUES ('Qual é o sonho de Naruto?', 1);
INSERT INTO tb_questions (statement, quiz_id) VALUES ('Qual é o golpe do Luffy?', 2);
INSERT INTO tb_questions (statement, quiz_id) VALUES ('Qual é a Raposa de Nove Caudas selada em Naruto?', 1);
INSERT INTO tb_questions (statement, quiz_id) VALUES ('Qual é a vila de origem de Naruto?', 1);
INSERT INTO tb_questions (statement, quiz_id) VALUES ('Quem é o espadachim dos Chapéus de Palha?', 2);
INSERT INTO tb_questions (statement, quiz_id) VALUES ('Qual é o sonho de Luffy?', 2);

-- =====================================
-- OPTIONS (a ordem define os IDs: 1 a 16; o gabarito fica só no banco, nunca na API)
-- =====================================
INSERT INTO tb_options (text, correct, question_id) VALUES ('Ser Hokage', true, 1);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Ser Rei dos Piratas', false, 1);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Gomu Gomu no Pistol', true, 2);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Kamehameha', false, 2);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Kurama', true, 3);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Shukaku', false, 3);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Matatabi', false, 3);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Vila da Folha (Konoha)', true, 4);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Vila da Areia (Suna)', false, 4);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Vila da Névoa (Kiri)', false, 4);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Roronoa Zoro', true, 5);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Sanji', false, 5);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Usopp', false, 5);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Ser o Rei dos Piratas', true, 6);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Ser o melhor espadachim do mundo', false, 6);
INSERT INTO tb_options (text, correct, question_id) VALUES ('Ser Hokage', false, 6);

-- =====================================
-- SCORES (coerentes com a regra de pontuação e com os números da tentativa)
-- Colunas: points, correct_answers (acertos), answered_questions (respostas enviadas),
--          total_questions (questões do quiz na tentativa), user_id, quiz_id.
--   Naruto  no Quiz 1: 3 acertos de 3 respostas x 10 = 30
--   Sasuke  no Quiz 1: 2 acertos de 3 respostas x 10 = 20
--   Luffy   no Quiz 2: 2 acertos de 3 respostas x 20 = 40
-- =====================================
INSERT INTO tb_scores (points, correct_answers, answered_questions, total_questions, user_id, quiz_id) VALUES (30, 3, 3, 3, 1, 1);
INSERT INTO tb_scores (points, correct_answers, answered_questions, total_questions, user_id, quiz_id) VALUES (20, 2, 3, 3, 2, 1);
INSERT INTO tb_scores (points, correct_answers, answered_questions, total_questions, user_id, quiz_id) VALUES (40, 2, 3, 3, 3, 2);

-- =====================================
-- PERFIS: todo usuário tem um perfil (vazio por padrão).
-- Este comando é IDEMPOTENTE: só cria o perfil de quem ainda não tem. Cobre os usuários de exemplo e qualquer
-- usuário antigo sem perfil (a leitura de perfil nunca cria registros). Em bancos persistentes, a mesma instrução
-- deve ir numa migração (Fase 3).
-- =====================================
INSERT INTO user_profiles (user_id)
SELECT u.id FROM tb_users u WHERE NOT EXISTS (SELECT 1 FROM user_profiles p WHERE p.user_id = u.id);

-- =====================================
-- RELACIONAMENTO N:N (User - Quiz Favoritos)
-- =====================================
INSERT INTO tb_user_favorite_quizzes (user_id, quiz_id) VALUES (1, 1);
INSERT INTO tb_user_favorite_quizzes (user_id, quiz_id) VALUES (3, 2);
