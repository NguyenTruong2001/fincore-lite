ALTER TABLE fincore.system_metadata
    ADD COLUMN created_by VARCHAR(100),
    ADD COLUMN updated_at TIMESTAMPTZ,
    ADD COLUMN updated_by VARCHAR(100),
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE fincore.system_metadata
SET created_by = 'system:migration',
    updated_at = created_at,
    updated_by = 'system:migration'
WHERE created_by IS NULL
   OR updated_at IS NULL
   OR updated_by IS NULL;

ALTER TABLE fincore.system_metadata
    ALTER COLUMN created_by SET NOT NULL,
ALTER COLUMN updated_at SET NOT NULL,
    ALTER COLUMN updated_by SET NOT NULL;

ALTER TABLE fincore.system_metadata
    ADD CONSTRAINT ck_system_metadata_created_by_not_blank
        CHECK (BTRIM(created_by) <> ''),

    ADD CONSTRAINT ck_system_metadata_updated_by_not_blank
        CHECK (BTRIM(updated_by) <> ''),

    ADD CONSTRAINT ck_system_metadata_updated_at_not_before_created_at
        CHECK (updated_at >= created_at),

    ADD CONSTRAINT ck_system_metadata_version_non_negative
        CHECK (version >= 0);

COMMENT ON COLUMN fincore.system_metadata.created_by IS
    'Stable actor identifier that created the metadata row.';

COMMENT ON COLUMN fincore.system_metadata.updated_at IS
    'UTC timestamp of the latest successful update.';

COMMENT ON COLUMN fincore.system_metadata.updated_by IS
    'Stable actor identifier that performed the latest update.';

COMMENT ON COLUMN fincore.system_metadata.version IS
    'Optimistic locking version managed by Hibernate.';