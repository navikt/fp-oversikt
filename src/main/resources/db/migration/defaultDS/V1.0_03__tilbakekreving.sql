CREATE TABLE TILBAKEKREVING
(
    ID                        NUMERIC,
    SAKSNUMMER                VARCHAR(50) UNIQUE,
    JSON                      JSONB
);
ALTER TABLE TILBAKEKREVING ADD CONSTRAINT PK_TILBAKEKREVING PRIMARY KEY (ID);
CREATE SEQUENCE SEQ_TILBAKEKREVING MINVALUE 1000000 START WITH 1000000 INCREMENT BY 50 NO CYCLE;


