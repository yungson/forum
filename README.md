
## Introduction
![Architecture of Bee Community Application](https://www.processon.com/view/link/62a577285653bb5256cc92f3)

## Installation

A server with >4GB memory is recommended. This forum application can be easily deployed using docker.

### config the environment

### checking the status of each service

## create network

```shell
 docker network create -d bridge my-net
```

Check the status of each docker container as follows

#### mysql

```shell
root@e7d6d35b87eb:/# mysql -uroot -p
Enter password: 
Welcome to the MySQL monitor.  Commands end with ; or \g.
Your MySQL connection id is 9
Server version: 8.0.29 MySQL Community Server - GPL

Copyright (c) 2000, 2022, Oracle and/or its affiliates.

Oracle is a registered trademark of Oracle Corporation and/or its
affiliates. Other names may be trademarks of their respective
owners.

Type 'help;' or '\h' for help. Type '\c' to clear the current input statement.

mysql> show databases;
+--------------------+
| Database           |
+--------------------+
| forum              |
| information_schema |
| mysql              |
| performance_schema |
| sys                |
+--------------------+
5 rows in set (0.00 sec)

mysql>
```
Alternatively,  you can navigate to localhost:8081 and login to the adminer portal as `root`
to check whether the `forum` database exists or not.

#### elastic search
``` shell
$curl -X GET "localhost:9200/_cat/nodes?v&pretty"
ip         heap.percent ram.percent cpu load_1m load_5m load_15m node.role   master name
172.18.0.3           27          94   1    0.11    0.05     0.08 cdfhilmrstw *      be6aa04613a7
```

**elastic search plugin(optional)**

check if the additional plugins is correctly mounted. `ik` is a word segmentation tool for chinese. 
```shell
$docker exec forum_es ls /usr/share/elasticsearch/plugins/ik
commons-codec-1.9.jar
commons-logging-1.2.jar
config
elasticsearch-analysis-ik-7.17.3.jar
elasticsearch-analysis-ik-7.17.3.zip
httpclient-4.5.2.jar
httpcore-4.4.4.jar
plugin-descriptor.properties
plugin-security.policy
```

#### Redis
```shell
$docker exec -it forum_redis bash 
root@1612cd80535c:/data# redis-cli
127.0.0.1:6379> select 1
OK
127.0.0.1:6379[1]> set test:count 1
OK
127.0.0.1:6379[1]>
```


#### Kafka

```shell
$docker exec -it forum_kafka bash 
I have no name!@ea619513946f:/$ kafka-topics.sh --create --bootstrap-server localhost:9092 --replication-factor 1 --partitions 1 --topic test
Created topic test.
I have no name!@ea619513946f:/$ kafka-console-producer.sh --broker-list localhost:9092 --topic test
>hello
>world!
>
```

open another terminal and you will receive two messages `hello` and `world!`
```shell
$docker exec -it forum_kafka bash
I have no name!@ea619513946f:/$ kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
hello
world!
```

### download and packaging

git clone the project and using the command below to package
```shell
docker run -it --rm --name my-maven-project -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven maven mvn clean package  -Dmaven.test.skip=true
```


You need to set the following environment variables accordingly to make it work
```shell
 export datasource_password=your_root_password_for_mysql
 export qqmail_authorization_code=your_mail_actuorization_code_or_password
 export qiniu_access_key=your_cdn_access_key
 export qiniu_access_secret=your_cdn_access_secret
```