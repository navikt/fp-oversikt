package no.nav.foreldrepenger.oversikt.server;

import java.sql.Types;

import org.hibernate.dialect.PostgreSQL94Dialect;

public class PostgreSQLDialect extends PostgreSQL94Dialect {

    public PostgreSQLDialect() {
        super();
        this.registerColumnType(Types.OTHER, "jsonb");
    }
}
