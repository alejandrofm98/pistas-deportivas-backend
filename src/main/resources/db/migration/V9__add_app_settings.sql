CREATE TABLE app_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value TEXT NOT NULL DEFAULT ''
);

INSERT INTO app_settings (setting_key, setting_value) VALUES ('email_bcc', '');
