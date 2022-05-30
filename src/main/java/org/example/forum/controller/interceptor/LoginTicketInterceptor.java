package org.example.forum.controller.interceptor;

import org.example.forum.entity.LoginTicket;
import org.example.forum.entity.User;
import org.example.forum.service.UserService;
import org.example.forum.util.CookieUtil;
import org.example.forum.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

@Component
public class LoginTicketInterceptor implements HandlerInterceptor {

    @Autowired
    private UserService userService;
    @Autowired
    private HostHolder hostHolder;
    // 在interceptor里面我们无法通过CookieValue注解获取http请求里面的值，但是可以通过HttpServletRequest request
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String ticket = CookieUtil.getValue(request, "ticket");
        if (ticket!=null){
            LoginTicket loginTicket = userService.findLoginTicket(ticket);
            if(loginTicket!=null && loginTicket.getStatus() == 0 && loginTicket.getExpired().after(new Date())){
                User user = userService.findUserById(loginTicket.getUserId());
                // 在本次请求中持有user，但是由于HTTP是多线程的，我们只能通过java的threadLocal方式去"线程隔离"地存到对应线程中去，每个线程互不干扰
                // 因此我们创建里Forum.util.HostHolder来帮助我们在这次请求中持有user
                // 我们在此处将user存入当前线程t对应的map里，只要本次请求还没有处理完，该线程就一直持有user,当服务器响应后才消失
                hostHolder.setUser(user);
            }
        }
        return true;
    }

    // user持有之后什么时候用呢？就是在模版引擎之前，因为要把这个user传递给模版引擎， 而postHandler就是在模版引擎之前
    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        User user = hostHolder.getUser();
        if (user!= null && modelAndView!=null){
            modelAndView.addObject("loginUser", user);
        }
    }

    //在请求全部执行完之后就可以将user remove 掉

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        hostHolder.clear();
    }
}
