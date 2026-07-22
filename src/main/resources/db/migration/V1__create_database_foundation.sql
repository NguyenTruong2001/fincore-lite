CREATE TABLE fincore.system_metadata
(
    metadata_key   VARCHAR(100) NOT NULL,
    metadata_value VARCHAR(500) NOT NULL,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_system_metadata
        PRIMARY KEY (metadata_key),

    CONSTRAINT ck_system_metadata_key_not_blank
        CHECK (BTRIM(metadata_key) <> ''),

    CONSTRAINT ck_system_metadata_value_not_blank
        CHECK (BTRIM(metadata_value) <> '')
);

COMMENT
ON TABLE fincore.system_metadata IS
    'Technical metadata used to verify database initialization and migration.';

COMMENT
ON COLUMN fincore.system_metadata.metadata_key IS
    'Unique technical metadata key.';

COMMENT
ON COLUMN fincore.system_metadata.metadata_value IS
    'Technical metadata value.';

COMMENT
ON COLUMN fincore.system_metadata.created_at IS
    'UTC timestamp at which the metadata row was created.';

INSERT INTO fincore.system_metadata
(metadata_key,
 metadata_value)
VALUES ('database.foundation.version',
        '1');