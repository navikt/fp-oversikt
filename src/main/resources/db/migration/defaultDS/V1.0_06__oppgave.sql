CREATE TABLE OPPGAVE
(
    ID                        UUID,
    SAKSNUMMER                VARCHAR(50),
    TYPE                      VARCHAR(100) NOT NULL,
    OPPRETTET_TID             TIMESTAMP(3) WITHOUT TIME ZONE NOT NULL,
    AVSLUTTET_TID             TIMESTAMP(3) WITHOUT TIME ZONE
);
ALTER TABLE OPPGAVE ADD CONSTRAINT PK_OPPGAVE PRIMARY KEY (ID);
CREATE INDEX IDX_OPPGAVE_1 ON OPPGAVE (SAKSNUMMER);
