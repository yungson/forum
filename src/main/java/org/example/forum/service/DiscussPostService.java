package org.example.forum.service;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.example.forum.dao.DiscussPostMapper;
import org.example.forum.entity.DiscussPost;
import org.example.forum.util.SensitiveFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.concurrent.TimeUnit;


/*
* 无论service方法多简单，都要通过Controller调Service， Service调Mapper/DAO， 不要跳过，开发的可维护性和以后可能的升级，规范等 */
@Service
public class DiscussPostService {

    private static final Logger logger = LoggerFactory.getLogger(DiscussPostService.class);
    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Value("${caffeine.posts.max-size}")
    private int maxSize;
    @Value("${caffeine.posts.expire-seconds}")
    private int expireSeconds;

    // caffeine核心接口： Cache, LoadingCache, AsyncLoadingCache
    // 帖子列表缓存
    private LoadingCache<String, List<DiscussPost>> postListCache;
    // 帖子总数缓存
    private LoadingCache<Integer,Integer> postRowsCache;

    @PostConstruct
    public void init() {
        // 初始化帖子列表缓存
        postListCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<String, List<DiscussPost>>() {
                    @Override
                    public @Nullable List<DiscussPost> load(String key) throws Exception {
                        if (key == null || key.length() == 0){
                            throw new IllegalArgumentException("key can not be null");
                        }
                        String[] params = key.split(":");
                        if (params == null || params.length !=2){
                            throw new IllegalArgumentException("incorrect key format");
                        }
                        int offset = Integer.valueOf(params[0]);
                        int limit = Integer.valueOf(params[1]);
                        // 二级缓存 redis
                        logger.debug("loading discuss posts from mysql database ");
                        return discussPostMapper.selectDiscussPosts(0, offset, limit, 1);
                    }
                });
        // 初始化帖子总数
        postRowsCache = Caffeine.newBuilder()
                .maximumSize(maxSize)
                .expireAfterWrite(expireSeconds, TimeUnit.SECONDS)
                .build(new CacheLoader<Integer, Integer>() {
                    @Override
                    public @Nullable Integer load(Integer key) throws Exception {
                        logger.debug("loading post rows from mysql database ");
                        return discussPostMapper.selectDiscussPostRows(key);
                    }
                });

    }
    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit, int orderMode){
        if (userId == 0 && orderMode == 1){
            return postListCache.get(offset+":"+limit);
        }
        logger.debug("loading discuss posts from database...");
        return discussPostMapper.selectDiscussPosts(userId, offset, limit, orderMode);
    }

    public int findDiscussPostRows(int userId){
        if (userId == 0){
            return postRowsCache.get(userId);
        }
        logger.debug("loading post rows from database...");
        return discussPostMapper.selectDiscussPostRows(userId);
    }

    public int addDiscussPost(DiscussPost post){
        if(post==null){
            throw new IllegalArgumentException("discussPost cannot be null!");
        }
        // 转义html标记 比如标题或内容里含<script> </script>的
        post.setTitle(HtmlUtils.htmlEscape(post.getTitle()));
        post.setContent(HtmlUtils.htmlEscape(post.getContent()));
        post.setTitle(sensitiveFilter.filter(post.getTitle()));
        post.setContent(sensitiveFilter.filter(post.getContent()));
        return discussPostMapper.insertDiscussPost(post);
    }

    public DiscussPost findDiscussPostById(int id){
        return discussPostMapper.selectDiscussPostById(id);
    }

    public int updateCommentCount(int id, int commentCount){
        return discussPostMapper.updateCommentCount(id, commentCount);
    }

    public int updateType(int id, int type){
        return discussPostMapper.updateType(id, type);
    }

    public int updateStatus(int id, int status){
        return discussPostMapper.updateStatus(id, status);
    }

    public int updateScore(int id, double score){
        return discussPostMapper.updateScore(id, score);
    }

}
