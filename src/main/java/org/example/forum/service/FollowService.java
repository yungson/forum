package org.example.forum.service;

import org.example.forum.entity.User;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class FollowService {

    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private UserService userService;

    public void follow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().add(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().add(followerKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    public void unfollow(int userId, int entityType, int entityId) {
        redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
                String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
                operations.multi();
                operations.opsForZSet().remove(followeeKey, entityId, System.currentTimeMillis());
                operations.opsForZSet().remove(followerKey, userId, System.currentTimeMillis());

                return operations.exec();
            }
        });
    }

    // 查询关注的实体的数量
    public long findFolloweeCount(int userId, int entityType){
        String followeeKey  = RedisKeyUtil.getFolloweeKey(userId, entityType);
        return redisTemplate.opsForZSet().zCard(followeeKey);
    }

    // 查询实体的粉丝的数量
    public long findFollowerCount(int entityType, int entityId){
        String followerKey = RedisKeyUtil.getFollowerKey(entityType, entityId);
        return redisTemplate.opsForZSet().zCard(followerKey);
    }

    // 查询当前用户是否已关注某个实体
    public boolean findHasFollowed(int userId, int entityType, int entityId){
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, entityType);
        System.out.println("userId = " + userId + ", entityType = " + entityType + ", entityId = " + entityId);
        return redisTemplate.opsForZSet().score(followeeKey, entityId) != null;
    }

    // Look up for the followee of somebody
    public List<Map<String, Object>> findFollowees(int userId, int offset, int limit) {
        String followeeKey = RedisKeyUtil.getFolloweeKey(userId, ForumConstant.ENTITY_TYPE_USER);
        Set<Integer> targetIds = redisTemplate.opsForZSet().reverseRange(followeeKey, offset, offset+limit-1); // for redis, end is included
        if (targetIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for(Integer targetId: targetIds){
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(targetId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followeeKey, targetId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

    // look up for the fans of somebody
    public List<Map<String, Object>> findFollowers(int userId, int offset, int limit){
        String followerKey = RedisKeyUtil.getFollowerKey(ForumConstant.ENTITY_TYPE_USER, userId);
        // redisTemplate 返回的Set<Integer> 是redis自己实现的， 是有序的。java自带的Set是无序的
        Set<Integer> fromIds = redisTemplate.opsForZSet().reverseRange(followerKey, offset, offset+limit-1);
        if ( fromIds == null) {
            return null;
        }
        List<Map<String, Object>> list = new ArrayList<>();
        for(Integer fromId: fromIds){
            Map<String, Object> map = new HashMap<>();
            User user = userService.findUserById(fromId);
            map.put("user", user);
            Double score = redisTemplate.opsForZSet().score(followerKey, fromId);
            map.put("followTime", new Date(score.longValue()));
            list.add(map);
        }
        return list;
    }

}
