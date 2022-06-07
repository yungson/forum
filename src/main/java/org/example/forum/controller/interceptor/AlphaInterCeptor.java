package org.example.forum.controller.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


public class AlphaInterCeptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(AlphaInterCeptor.class);
    //在Controller之前执行
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        logger.debug("preHandler: "+handler.toString());
        return HandlerInterceptor.super.preHandle(request, response, handler);
    }


    //在Controller之后执行(所以我们可以看到postHandle里面有modelAndView参数，controller之后才有这个东西，方便让interceptor做一些操作)
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        logger.debug("postHandler: "+handler.toString());
        HandlerInterceptor.super.postHandle(request, response, handler, modelAndView);
    }

    // 在程序的最后执行，也就是TemplateEngine之后执行。
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        logger.debug("afterCompletion:" +handler.toString());
        HandlerInterceptor.super.afterCompletion(request, response, handler, ex);
    }
}
