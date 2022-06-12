set -e pipefail
# docker rm -f $(docker ps -a -q)
# rm -rf /mnt/volume_sfo3_01/project/forum
docker network ls|grpe my-net
if [ $? -eq 0 ]; then
  echo Netowork my-net exist! Skip!
else
  docker network create -d bridge my-net
fi

export deploy_root=$1

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
export datasource_password=d50960f067142c59cc3bdac61b87759f
export server_ip=164.92.79.241
export qqmail_authorization_code=zujhxfscwcygdiag
export qiniu_access_key=AYNuEVq1TpUC9lTwbZYLAnR6nxCLdN5-_uidboBM
export qiniu_access_secret=KUygoc1blB8yY3eGwegSmGbZr5SYcCUsdya8oSiQ


mkdir -p $data_mysql $data_redis $data_elasticsearch $data_zookeeper $data_kafka  $data_tomcat/webapps $data_upload_dir $wk_image_storage

chmod -R 777  $data_mysql $data_redis $data_elasticsearch $data_zookeeper $data_kafka  $data_tomcat $data_upload_dir $wk_image_storage



if [ ! -d "$deploy_root/elasticsearch/ik" ]; then
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

docker compose  up
