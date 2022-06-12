CREATE DATABASE  `forum` DEFAULT CHARACTER SET utf8mb4;
flush privileges;
use forum;
source /opt/sql/init_schema.sql;
source /opt/sql/init_data.sql;
source /opt/sql/tables_mysql_innodb.sql;
