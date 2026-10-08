-- =====================================
-- USERS
-- =====================================
INSERT INTO users (username, email) VALUES ('Isaac', 'naruto@gmail.com');
INSERT INTO users (username, email) VALUES ('Tiago Otaku Fedido', 'sasuke@gmail.com');
INSERT INTO users (username, email) VALUES ('Monkey', 'luffy@onepiece.com');

-- =====================================
-- QUIZZES
-- =====================================
INSERT INTO quizzes (title, description) VALUES ('Naruto Shippuden Quiz', 'Teste os seus conhecimentos sobre o universo ninja!');
INSERT INTO quizzes (title, description) VALUES ('One Piece Quiz', 'Quanto sabe sobre os Piratas do Chapéu de Palha?');

-- =====================================
-- QUESTIONS
-- =====================================
-- Pergunta associada ao Quiz 1 (Naruto)
INSERT INTO questions (statement, quiz_id) VALUES ('Qual é o grande sonho de Naruto Uzumaki?', 1);
-- Pergunta associada ao Quiz 2 (One Piece)
INSERT INTO questions (statement, quiz_id) VALUES ('Qual é o nome do golpe mais clássico do Luffy?', 2);

-- =====================================
-- OPTIONS (Alternativas de resposta)
-- =====================================
-- Opções para a Pergunta 1
INSERT INTO options (text, correct, question_id) VALUES ('Ser Hokage', true, 1);
INSERT INTO options (text, correct, question_id) VALUES ('Ser o Rei dos Piratas', false, 1);

-- Opções para a Pergunta 2
INSERT INTO options (text, correct, question_id) VALUES ('Gomu Gomu no Pistol', true, 2);
INSERT INTO options (text, correct, question_id) VALUES ('Kamehameha', false, 2);

-- =====================================
-- SCORES (Pontuações para o Ranking)
-- =====================================
INSERT INTO scores (points, user_id) VALUES (150, 1);
INSERT INTO scores (points, user_id) VALUES (120, 2);
INSERT INTO scores (points, user_id) VALUES (200, 3);