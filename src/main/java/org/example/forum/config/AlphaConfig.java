package org.example.forum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.text.SimpleDateFormat;

@Configuration //标识这个类是个配置类，用于装配第三方的bean
public class AlphaConfig {

    @Bean //如果要装配第三方的bean, 要加上@bean注解在方法之前，方法名simpleDateFormat就是bean的名字
    public SimpleDateFormat simpleDateFormat() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }

}
