CREATE TABLE account (
  id INT NOT NULL AUTO_INCREMENT,
  name VARCHAR(50),
  amount FLOAT,

  CONSTRAINT pk_t_account PRIMARY KEY (ID)
);

CREATE TABLE transfer (
  id INT NOT NULL AUTO_INCREMENT,
  sender_account_id INT NOT NULL,
  receiver_account_id INT NOT NULL,
  status INT NOT NULL,
  amount FLOAT,
  processing_id VARCHAR(50),
  processing_start DATETIME,
  processing_end DATETIME,

  CONSTRAINT pk_t_transfer PRIMARY KEY (id),
  CONSTRAINT fk_t_transfer_sender_account_id FOREIGN KEY (sender_account_id)
        REFERENCES account(id) ON DELETE CASCADE,
  CONSTRAINT k_t_receiver_account_id FOREIGN KEY (receiver_account_id)
        REFERENCES account(id) ON DELETE CASCADE,
);