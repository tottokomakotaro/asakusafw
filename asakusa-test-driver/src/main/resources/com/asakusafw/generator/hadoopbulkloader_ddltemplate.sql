DROP TABLE IF EXISTS @TABLE_NAME@_RL;
CREATE TABLE @TABLE_NAME@_RL (
  SID BIGINT PRIMARY KEY ,
  JOBFLOW_SID BIGINT NULL
) ENGINE=InnoDB;
