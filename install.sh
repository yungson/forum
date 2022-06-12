#!/bin/bash
# usage 
# docker rm -f $(docker ps -a -q)
# rm -rf /mnt/volume_sfo3_01/project/forum


if [ $# -ne 2 ];
then
    echo usage: sh install.sh \<server_ip\> \<deploy_directory\>
    exit
fi


export server_ip=$1
export deploy_root=$2
echo the deployment dir is: $deploy_root


cat>deploy/nginx.conf<<EOF

user  nginx;
worker_processes  auto;

error_log  /var/log/nginx/error.log notice;
pid        /var/run/nginx.pid;


events {
    worker_connections  1024;
}


http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format  main  '\$remote_addr - \$remote_user [\$time_local] "\$request" '
                      '\$status \$body_bytes_sent "\$http_referer" '
                      '"\$http_user_agent" "\$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    keepalive_timeout  65;

    #gzip  on;

    include /etc/nginx/conf.d/*.conf;
    upstream myserver{
        server forum_tomcat:8080 max_fails=3 fail_timeout=30s;  # 最多失败3次，每次30s timeous
    }
    server {
        listen 80;
        server_name $server_ip;
        location / {
                #表示转交给myserver处理
                proxy_pass http://myserver; 
        }
    }
}
EOF

set -e pipefail

docker network ls|grep my-net
if [ $? -eq 0 ]; then
  echo Netowork my-net exist! Skip!
else
  docker network create -d bridge my-net
fi

export config_dir=$PWD/deploy
export data_mysql=${deploy_root}/mysql
export data_redis=${deploy_root}/redis
export data_elasticsearch=${deploy_root}/elasticsearch
export data_zookeeper=${deploy_root}/zookeeper
export data_kafka=${deploy_root}/kafka
export data_system=${deploy_root}/system
export data_tomcat=${deploy_root}/tomcat
export data_upload_dir=${data_system}/upload
export wk_image_storage=${data_system}/wkhtmltopdf
export wk_image_command="docker run -it --rm -v ${wk_image_storage}:${wk_image_storage} --entrypoint /wkhtmltox/bin/wkhtmltoimage osones/wkhtmltopdf"
export datasource_password=${datasource_password}
export qqmail_authorization_code=${qqmail_authorization_code}
export qiniu_access_key=${qiniu_access_key}
export qiniu_access_secret=${qiniu_access_secret}


mkdir -p $data_mysql $data_redis $data_elasticsearch $data_zookeeper $data_kafka  $data_tomcat/webapps $data_upload_dir $wk_image_storage

chmod -R 777  $data_mysql $data_redis $data_elasticsearch $data_zookeeper $data_kafka  $data_tomcat $data_upload_dir $wk_image_storage

if [ ! -d "$deploy_root/elasticsearch/ik" ]; then
	echo $deploy_root/elasticsearch/ik not existing, downloading...
	wget https://github.com/medcl/elasticsearch-analysis-ik/releases/download/v7.17.3/elasticsearch-analysis-ik-7.17.3.zip \
	-O $data_elasticsearch/elasticsearch-analysis-ik-7.17.3.zip && \
	unzip $data_elasticsearch/elasticsearch-analysis-ik-7.17.3.zip -d $data_elasticsearch/ik 
fi

if [ ! -f "$data_tomcat/webapps/ROOT.war" ]; then
	sed -i "s#spring.profiles.active=develop#spring.profiles.active=production#g" src/main/resources/application.properties && \
	docker run -it --rm --name my-maven-project -v "$(pwd)":/usr/src/mymaven -w /usr/src/mymaven maven mvn clean package \
	-Dmaven.test.skip=true -Dactive.profile=production
	cp target/ROOT.war $data_tomcat/webapps/
fi

docker compose  up -d
