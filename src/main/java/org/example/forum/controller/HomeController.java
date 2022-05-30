package org.example.forum.controller;

import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.Page;
import org.example.forum.entity.User;
import org.example.forum.service.DiscussPostService;
import org.example.forum.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class HomeController {

    @Autowired
    private UserService userService;

    @Autowired
    private DiscussPostService discussPostService;

    @RequestMapping(path="/index", method= RequestMethod.GET)
    public String getIndexPage(Model model, Page page){
        page.setRows(discussPostService.findDiscussPostRows(0));
        System.out.println(page.getRows());
        page.setPath("/index");
        List<DiscussPost> list =  discussPostService.findDiscussPosts(0,page.getOffset(), page.getLimit());
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(list != null){
            for(DiscussPost post: list){
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                User user = userService.findUserById(post.getUserId());
                map.put("user", user);
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        // 如果我们想在index里面也使用page，理论上应该像discussPosts一样加到属性里，但是我们不用
        // 因为方法在调用前，SpringMVC会自动实例化Model和Page，并将Page注入Model, 因此可以在thymeleaf中直接访问Page
        return "/index";
    }
}