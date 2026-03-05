-- MEMBER 테이블의 role 컬럼에 'USER', 'ADMIN'만 들어올 수 있도록 체크 제약 조건 추가
ALTER TABLE MEMBER ADD CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN'));
