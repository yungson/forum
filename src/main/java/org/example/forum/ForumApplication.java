package org.example.forum;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ForumApplication {

	public static void main(String[] args) {
		/* spring 容器是自动创建的,创建以后会自动去扫描某些包下的某些bean, 将这些bean装配到容器中
		 * spring 启动的时候需要配置的，@SpringBootApplication所标识的类就是spring的配置类(相当于配置文件)
		 * */
		SpringApplication.run(ForumApplication.class, args);

	}

}
