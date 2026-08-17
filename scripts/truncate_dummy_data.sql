-- 개발용 MySQL(elixir_codex)에 CommandLineRunner + 반복 테스트 실행으로 누적된
-- 더미 데이터를 비우는 스크립트. 직접 실행할 것.
--
-- 현재 이 5개 테이블 사이에는 실제 외래키 제약이 없는 걸 확인했지만(각자 ownerId만
-- Long으로 들고 있을 뿐 서로 참조하지 않음), 나중에 FK가 추가되더라도 안전하게 동작하도록
-- FOREIGN_KEY_CHECKS를 잠깐 꺼둔 채로 TRUNCATE한다.

SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE elixir_card;
TRUNCATE TABLE attendance_log;
TRUNCATE TABLE furniture_reward;
TRUNCATE TABLE supplement_log;
TRUNCATE TABLE ingredient_card;

SET FOREIGN_KEY_CHECKS = 1;
