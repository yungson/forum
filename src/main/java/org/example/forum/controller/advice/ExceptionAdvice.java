package org.example.forum.controller.advice;

import org.example.forum.util.ForumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.PrintWriter;

@ControllerAdvice(annotations = Controller.class) //表示spring容器会扫描含有Controller注解的bean
public class ExceptionAdvice {

    private static final Logger logger = LoggerFactory.getLogger(ExceptionAdvice.class);

    @ExceptionHandler({Exception.class}) //处理所有异常，所有异常都是Exception的子类
    public void handleException(Exception e, HttpServletRequest request, HttpServletResponse response) throws IOException {
        logger.error("Internal Server Error:"+e.getMessage());
        for(StackTraceElement element: e.getStackTrace()){
            logger.error(element.toString());
        }

        String xRequestedWith = request.getHeader("x-requested-with");
        if("XMLHttpRequest".equals(xRequestedWith)){ // 如果请求里面的x-requested-width=XMLHttpRequest说明是一个异步请求， 异步请求异常情况下，我们要返回一个json字符串，重定向到error页面是没有意义的
            response.setContentType("application/plain;charset=utf-8");
            PrintWriter writer = response.getWriter();
            logger.error("异步请求异常");
            writer.write(ForumUtil.getJSONString(1,"服务器异常"));
        } else{
            response.sendRedirect(request.getContextPath()+"/error");
        }
    }

}
