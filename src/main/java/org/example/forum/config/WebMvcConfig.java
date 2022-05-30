package org.example.forum.config;

import org.example.forum.controller.interceptor.AlphaInterCeptor;
import org.example.forum.controller.interceptor.LoginTicketInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// 与其他的配置类不同，其他的配置类我们通常都是在装配一个bean
// 而拦截器的配置类实际上在实现一个配置类
// 我们只有实现了这个配置类spring才能构正确的将controller/interceptor/AlphaInterceptor管理
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private AlphaInterCeptor alphaInterCeptor;

    @Autowired
    LoginTicketInterceptor loginTicketInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(alphaInterCeptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png","/**/*.jpg" , "/**/*.jpeg") //静态资源不需要拦截，**排除掉所有目录的css
                .excludePathPatterns("/register", "/login"); // 需要拦截的放到这里, 比如在访问login page的时候就可以看到拦截器的preHandler, postHandler, 等输出，说明确实拦截到了
        registry.addInterceptor(loginTicketInterceptor)
                .excludePathPatterns("/**/*.css", "/**/*.js", "/**/*.png","/**/*.jpg" , "/**/*.jpeg"); //静态资源不需要拦截，**排除掉所有目录的css
        // loginTicketInterceptor我们设置拦截所有请求

    }
}
