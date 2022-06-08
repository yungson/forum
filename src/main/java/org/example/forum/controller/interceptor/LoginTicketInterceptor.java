package org.example.forum.controller.interceptor;

import org.example.forum.entity.LoginTicket;
import org.example.forum.entity.User;
import org.example.forum.service.UserService;
import org.example.forum.util.CookieUtil;
import org.example.forum.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
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
                // 构建用户认证的结果，存入SecurityContext, 使得之后Spring Security用这个进行授权
                Authentication authentication = new UsernamePasswordAuthenticationToken(
                        user, user.getPassword(), userService.getAuthorities(user.getId()));
                SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
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
//        SecurityContextHolder.clearContext();
/* 不能在此处clearContext否则会出现即使登录成功，连续访问需要权限的资源会仍然需要登录的界面
* SecurityContextPersistenceFilter.doFilter(HttpServletRequest, HttpServletResponse, FilterChain)，
* 在这个Spring Security提供的过滤器的doFilter()方法中可以看到在过滤器中SecurityContext实例是从当前请求的session中获取的，
* 而chain.doFilter(req,res)执行完后会从SecurityContextHolder中获取context重新保存到session，
* 如果在Interceptor中执行了SecurityContextHolder.clearContext()，下次请求到服务器后，
* 这个过滤器从session中获取的SecurityContext就是null，最后导致的结果是你如果你访问的是需要登录的路径，
* 你会发现你登录成功了，但是一访问需要权限的资源又会重新登录，
* 所以在LoginInterceptor的afterCompletion()中其实不能执行SecuriContextHolder.clear()。
* 实际上上面说的那个过滤器的doFilter()中会执行一次clear()操作，根本不需要我们手动执行，如果你手动执行，
* 反而会出问题，因为你在interceptor中如果执行clear()，这个过滤器之后会从holder中获取并存到session，
* 导致每次存的都是null，你登录了，然后你又把登录信息清空了，导致每一次登录都是做的无用功，或者说你每访问一次需要登录的资源就需要登录一次。
* 这个Holder和我们定义的UserHolder其实不是完全一样的，它依赖session，session维护了它的状态，
* 如果只是像视频中这样做，浏览器关闭再打开就是新的Session，就要重新登录，这样一来，和直接将user存在session中没什么区别了。*/
    }
}
