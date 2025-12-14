DELETE FROM likes;
DELETE FROM film_genre;
DELETE FROM films;
DELETE FROM users;
DELETE FROM genres;
DELETE FROM mpa_rating;
DELETE FROM review_likes;
DELETE FROM reviews;

ALTER TABLE reviews ALTER COLUMN review_id RESTART WITH 1;
ALTER TABLE users ALTER COLUMN user_id RESTART WITH 1;
ALTER TABLE films ALTER COLUMN film_id RESTART WITH 1;

INSERT INTO mpa_rating (rating_id, name) VALUES
  (1, 'G'),
  (2, 'PG'),
  (3, 'PG-13'),
  (4, 'R'),
  (5, 'NC-17');

INSERT INTO genres (genre_id, name) VALUES
  (1, 'Комедия'),
  (2, 'Драма'),
  (3, 'Мультфильм'),
  (4, 'Триллер'),
  (5, 'Документальный'),
  (6, 'Боевик');

INSERT INTO films (film_id, name, description, release_date, duration, mpa_id) VALUES
  (1, 'Фильм А', 'Описание фильма А', '2000-01-01 00:00:00', 100, 1),
  (2, 'Фильм Б', 'Описание фильма Б', '2001-01-01 00:00:00', 110, 2);

INSERT INTO film_genre (film_id, genre_id) VALUES
  (1, 1),
  (2, 2),
  (2, 4);

INSERT INTO users (user_id, email, login, name, birthday) VALUES
  (1, 'user1@test.com', 'user1', 'Пользователь 1', '1990-01-01'),
  (2, 'user2@test.com', 'user2', 'Пользователь 2', '1995-01-01');

INSERT INTO likes (user_id, film_id) VALUES
  (1, 1),
  (2, 1),
  (1, 2);

INSERT INTO reviews (content, is_positive, user_id, film_id, useful)
VALUES ('Отличный фильм!', true, 1, 1, 10);

INSERT INTO reviews (content, is_positive, user_id, film_id, useful)
VALUES ('Не понравилось', false, 2, 1, 5);

INSERT INTO reviews (content, is_positive, user_id, film_id, useful)
VALUES ('Средний фильм', true, 2, 2, 2);

INSERT INTO reviews (content, is_positive, user_id, film_id, useful)
VALUES ('Лучший фильм года!', true, 1, 2, 15);

INSERT INTO review_likes (review_id, user_id, is_like)
VALUES (1, 2, true);
INSERT INTO review_likes (review_id, user_id, is_like)
VALUES (2, 1, false);