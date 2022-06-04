package org.example.forum.controller.interceptor;

import org.example.forum.entity.User;
import org.example.forum.service.MessageService;
import org.example.forum.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class MessageInterceptor implements HandlerInterceptor {

    @Autowired
    private HostHolder hostHolder;
    @Autowired
    private MessageService messageService;


    @Override
    public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, ModelAndView modelAndView) throws Exception {
        // postHandle 在controller之后执行就可以
        User user = hostHolder.getUser();
        if (user != null  && modelAndView != null){
            int TotalUnreadLetterCount = messageService.findUnreadLetterCount(user.getId(), null);
            int TotalUnreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), null);
            modelAndView.addObject("unread", TotalUnreadLetterCount+TotalUnreadNoticeCount);
        }
    }
}
