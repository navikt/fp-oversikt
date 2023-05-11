CREATE INDEX aktør_hash_index ON sak USING HASH ((json->>'aktørId'));
