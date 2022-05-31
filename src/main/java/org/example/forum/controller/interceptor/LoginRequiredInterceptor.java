package org.example.forum.controller.interceptor;

import org.example.forum.annotation.LoginRequired;
import org.example.forum.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Method;

@Component
public class LoginRequiredInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if(handler instanceof HandlerMethod){ // Object参数是拦截的目标，我们要判断以下它是不是方法，只有方法我们才需要处理，因为拦截器所有的资源都拦截包括静态资源，静态资源不用处理
            HandlerMethod handlerMethod = (HandlerMethod) handler;
            Method method = handlerMethod.getMethod(); //
            LoginRequired loginRequired = method.getAnnotation(LoginRequired.class); // 通过反射来取到方法上的LoginRequired注解
            if(loginRequired!=null && hostHolder.getUser()==null ){ // 也就是此方法需要登录才能访问的,但是同时user是null(未登录) //也就是我们做了进一步筛选，只处理有LoginRequired注解的访问，其他访问不处理
                response.sendRedirect(request.getContextPath()+"/login");
                return false;
            }
        }
        return true;
    }
}
