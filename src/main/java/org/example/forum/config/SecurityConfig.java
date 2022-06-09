package org.example.forum.config;

import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@Configuration
public class SecurityConfig implements ForumConstant {

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().antMatchers("/resources/**");
    }
    
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {

        http.authorizeRequests()
                .antMatchers(
                        "/user/setting",
                        "/user/upload",
                        "/discuss/add",
                        "/notice/**",
                        "/letter/**",
                        "/comment/add/**",
                        "/like",
                        "/follow",
                        "/unfollow")
                .hasAnyAuthority(AUTHORITY_USER, AUTHORITY_ADMIN, AUTHORITY_MODERATOR)
                .antMatchers("/discuss/top", "/discuss/credit").hasAnyAuthority(AUTHORITY_MODERATOR)
                .antMatchers("/discuss/delete","/data/**", "/actuator/**").hasAnyAuthority(AUTHORITY_ADMIN)
                .anyRequest().permitAll() //除了上面的请求意外其他所有请求都允许
                .and().csrf().disable(); //禁用掉csrf的验证

        // 在发生异常的时候如何处理，需要分同步请求和异步请求
        http.exceptionHandling()
                .authenticationEntryPoint(new AuthenticationEntryPoint() { //没有登录
                    @Override
                    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
                        System.out.println("request = " + request.getHeader("x-requested-with"));
                        String requestHeader = request.getHeader("x-requested-with");
                        System.out.println("context内容:"+SecurityContextHolder.getContext());
                        if ("XMLHttpRequest".equals(requestHeader)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(ForumUtil.getJSONString(403, "请登录！"));
                        } else {
                            response.sendRedirect(request.getContextPath()+"/login");
                        }
                    }
                })
                .accessDeniedHandler(new AccessDeniedHandler() { // 权限不足
                    @Override
                    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
                        String requestHeader = request.getHeader("x-requested-with");
                        if ("XMLHttpRequest".equals(requestHeader)){
                            response.setContentType("application/plain;charset=utf-8");
                            PrintWriter writer = response.getWriter();
                            writer.write(ForumUtil.getJSONString(403, "无权限！"));
                        } else {
                            response.sendRedirect(request.getContextPath()+"/denied");
                        }
                    }
                });
        // Security底层 默认会拦截/logout进行退出处理，此处需要覆盖默认的逻辑，才能执行我们自定义的退出代码
        http.logout().logoutUrl("/securitylogout"); // securitylogout并不存在，只是为了让security拦截退出失效
        // 除此之外，还需要将我们login认证的结果存到SecurityContext里面，因为Spring Security自带的认证使用SecurityContext里面的认证结果来进行后续的授权的
        // 但是我们是通过自己定义的方式进行login的，SecurityContext里面就不会有这个结果。Spring security是在servlet之前,那也在Controller之前，因此可以用拦截器进行存入
        return http.build();
    }


}
