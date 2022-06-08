package org.example.forum.quartz;

import org.example.forum.entity.DiscussPost;
import org.example.forum.service.DiscussPostService;
import org.example.forum.service.ElasticSearchService;
import org.example.forum.service.LikeService;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.RedisKeyUtil;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.BoundSetOperations;
import org.springframework.data.redis.core.RedisTemplate;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class PostScoreRefreshJob implements Job, ForumConstant {

    private static final Logger logger = LoggerFactory.getLogger(PostScoreRefreshJob.class);
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private DiscussPostService discussPostService;
    @Autowired
    private LikeService likeService;
    @Autowired
    private ElasticSearchService elasticSearchService;
    private static final Date epoch;
    static {
        try {
            epoch = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse("2022-01-01 00:00:00");
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        String redisKey = RedisKeyUtil.getPostScoreKey();
        BoundSetOperations operations = redisTemplate.boundSetOps(redisKey);

        if(operations.size() == 0){
            logger.info("[任务取消] 没有需要刷新分数的帖子！");
            return;
        }
        logger.info("[任务开始] 正在刷新帖子分数，"+operations.size());
        while(operations.size()>0){
            this.refresh((Integer) operations.pop());
        }
        logger.info("[任务结束] 刷新帖子分数完成！");
    }

    private void refresh(int postId){
        DiscussPost post = discussPostService.findDiscussPostById(postId);
        if (post == null){
            logger.error("帖子不存在， info="+postId); // 防止在刷新分数过程中帖子被删除
            return;
        }
        boolean credited = (post.getStatus()==1); //是否是精华帖
        int commentCount = post.getCommentCount();
        long likeCount = likeService.findEntityLikeCount(ENTITY_TYPE_POST, postId);
        // 计算权重
        double weight = credited?75:0+commentCount*10+likeCount*2;
        // 分数 = 帖子权重+距离天数
        double score = Math.log10(Math.max(weight, 1)) + (post.getCreateTime().getTime()-epoch.getTime())/(1000*3600*24);
        // 更新帖子分数
        discussPostService.updateScore(postId, score);
        // 同步ES搜索的数据
        post.setScore(score);
        try {
            elasticSearchService.saveDiscussPost(post);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
