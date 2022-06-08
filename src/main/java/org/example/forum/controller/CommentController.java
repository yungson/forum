package org.example.forum.controller;

import org.example.forum.entity.Comment;
import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.Event;
import org.example.forum.event.EventProducer;
import org.example.forum.service.CommentService;
import org.example.forum.service.DiscussPostService;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.HostHolder;
import org.example.forum.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.Date;

@Controller
@RequestMapping(path = "/comment")
public class CommentController implements ForumConstant {

    @Autowired
    private HostHolder hostholder;

    @Autowired
    private CommentService commentService;
    @Autowired
    private EventProducer eventProducer;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path = "/add/{discussPostId}", method = RequestMethod.POST)
    public String addComment(@PathVariable("discussPostId") int discussPostId, Comment comment){
        comment.setUserId(hostholder.getUser().getId()); // 没有登录情况咋办？
        comment.setStatus(0);
        comment.setCreateTime(new Date());
        commentService.addComment(comment);
        //
        Event event = new Event()
                .setTopic(TOPIC_COMMENT)
                .setUserId(hostholder.getUser().getId())
                .setEntityType(comment.getEntityType())
                .setEntityId(comment.getEntityId())
                .setData("postId", discussPostId);
        if(comment.getEntityType() == ENTITY_TYPE_POST) {
            DiscussPost target = discussPostService.findDiscussPostById(comment.getEntityId());
            event.setEntityUserId(target.getUserId());
        } else if (comment.getEntityType() == ENTITY_TYPE_COMMENT){
            Comment target = commentService.findCommentById(comment.getUserId());
            event.setEntityUserId(target.getUserId());
        }
        eventProducer.fireEvent(event);

        // 触发发帖事件
        if(comment.getEntityType() == ENTITY_TYPE_POST) { //只有回复给post的时候才加入ES数据库
             event = new Event()
                    .setTopic(TOPIC_PUBLISH)
                    .setUserId(comment.getId())
                    .setEntityType(ENTITY_TYPE_POST)
                    .setEntityId(discussPostId);
            eventProducer.fireEvent(event);
            // 计算帖子分数
            String redisKey = RedisKeyUtil.getPostScoreKey();
            redisTemplate.opsForSet().add(redisKey, discussPostId);
        }


        return "redirect:/discuss/detail/"+discussPostId;
    }
}
