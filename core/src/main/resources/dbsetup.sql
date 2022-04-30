CREATE TABLE IF NOT EXISTS shop_transaction
(
    id  MEDIUMINT NOT NULL AUTO_INCREMENT,
    t_type CHAR(6) NOT NULL,
    price   DOUBLE UNSIGNED  NOT NULL,
    amount   SMALLINT UNSIGNED  NOT NULL,
    item   CHAR(32)  NOT NULL,
    barter_item   CHAR(32),
    PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS shop_action
(
    ts TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    player_uuid     CHAR(36)  NOT NULL,
    owner_uuid     CHAR(36)  NOT NULL,
    shop_world   CHAR(48)  NOT NULL,
    shop_x   INT  NOT NULL,
    shop_y   INT  NOT NULL,
    shop_z   INT  NOT NULL,
    player_action     CHAR(8)  NOT NULL,
    transaction_id MEDIUMINT,
    FOREIGN KEY (transaction_id) REFERENCES shop_transaction(id)
);