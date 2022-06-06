package org.example.forum.controller;

import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.Page;
import org.example.forum.entity.SearchResult;
import org.example.forum.service.ElasticSearchService;
import org.example.forum.service.LikeService;
import org.example.forum.service.UserService;
import org.example.forum.util.ForumConstant;
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
public class SearchController implements ForumConstant {

    @Autowired
    private ElasticSearchService elasticSearchService;
    @Autowired
    private UserService userService;
    @Autowired
    private LikeService likeService;

    // search?keyword=xxx
    @RequestMapping(path = "/search", method = RequestMethod.GET)
    public String search(String keyword, Page page, Model model) {
        SearchResult searchResult = elasticSearchService.searchDiscussPost(keyword, page.getCurrent()-1, page.getLimit());
        List<Map<String, Object>> discussPosts = new ArrayList<>();
        if(searchResult.getList().size()!=0){
            for (DiscussPost post: searchResult.getList()){
                Map<String, Object> map = new HashMap<>();
                map.put("post", post);
                map.put("user", userService.findUserById(post.getUserId()));
                map.put("likeCount", likeService.findEntityLikeCount(ENTITY_TYPE_POST, post.getId()));
                discussPosts.add(map);
            }
        }
        model.addAttribute("discussPosts", discussPosts);
        model.addAttribute("keyword", keyword);
        // set pagination
        page.setPath("/search?keyword="+keyword);
        page.setRows((int) searchResult.getTotalHits());
        return "/site/search";
    }
}
