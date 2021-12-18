CREATE TABLE IF NOT EXISTS shop_action
(
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    player_uuid     CHAR(36)  NOT NULL,
    shop_location   CHAR(36)  NOT NULL,
    player_action     CHAR(7)  NOT NULL
);