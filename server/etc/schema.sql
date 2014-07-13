DROP TABLE IF EXISTS account;
DROP TABLE IF EXISTS device;

CREATE TABLE account (
	account_id			mediumint not null auto_increment,
	email					varchar(255),
   passwordSalt		tinyblob,
	passwordHash		tinyblob,
	primary key (account_id)
);
