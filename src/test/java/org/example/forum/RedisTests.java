package org.example.forum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.BoundValueOperations;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.test.context.ContextConfiguration;

import java.util.concurrent.TimeUnit;

@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class RedisTests {
    @Autowired
    private RedisTemplate redisTemplate;

    @Test
    public void testStrings(){
        String redisKey = "test:count";
        redisTemplate.opsForValue().set(redisKey, 1);
        System.out.println(redisTemplate.opsForValue().increment(redisKey));
        System.out.println(redisTemplate.opsForValue().decrement(redisKey));
    }

    @Test
    public void testHash(){
        String redisKey = "test:user";
        redisTemplate.opsForHash().put(redisKey,"id", 1);
        redisTemplate.opsForHash().put(redisKey, "username", "zhangsan");
        System.out.println(redisTemplate.opsForHash().get(redisKey, "id"));
        System.out.println(redisTemplate.opsForHash().get(redisKey, "username"));
    }
    @Test
    public void testList(){
        String redisKey = "test:ids";
        redisTemplate.opsForList().leftPush(redisKey, 100);
        redisTemplate.opsForList().leftPush(redisKey, 101);
        redisTemplate.opsForList().leftPush(redisKey, 102);
        System.out.println(redisTemplate.opsForList().size(redisKey));
        System.out.println(redisTemplate.opsForList().index(redisKey,1));
        System.out.println(redisTemplate.opsForList().range(redisKey,0, 2));
        System.out.println(redisTemplate.opsForList().leftPop(redisKey));
        System.out.println(redisTemplate.opsForList().rightPop(redisKey));
    }


    @Test
    public void testSet(){
        String redisKey = "test:teachers";
        redisTemplate.opsForSet().add(redisKey, "aaa","刘备","good");
        redisTemplate.opsForSet().add(redisKey, "bbb");
        redisTemplate.opsForSet().add(redisKey, "ccc");
        System.out.println(redisTemplate.opsForSet().size(redisKey));
        System.out.println(redisTemplate.opsForSet().distinctRandomMembers(redisKey, 2));

    }

    @Test
    public void testSortedSet(){
        String redisKey = "test:students";
        redisTemplate.opsForZSet().add(redisKey, "xiaoming", 10);
        redisTemplate.opsForZSet().add(redisKey, "xiaohong", 20);
        redisTemplate.opsForZSet().add(redisKey, "xiaogang", 15);
        redisTemplate.opsForZSet().add(redisKey, "miake", 40);
        System.out.println(redisTemplate.opsForZSet().zCard(redisKey));
        System.out.println(redisTemplate.opsForZSet().score(redisKey, "miake"));
        System.out.println(redisTemplate.opsForZSet().reverseRank(redisKey, "xiaohong"));
        System.out.println(redisTemplate.opsForZSet().reverseRange(redisKey, 0, 2));
    }

    @Test
    public void testKeys(){
        redisTemplate.delete("test:user");
        System.out.println(redisTemplate.hasKey("test:user"));
        redisTemplate.expire("test:students", 10, TimeUnit.SECONDS);
    }

    // 多次访问同一个key
    @Test
    public void testBoundOperations(){
        String redisKey = "test:count";
        BoundValueOperations operations = redisTemplate.boundValueOps(redisKey);
        operations.increment();
        operations.increment();
        operations.increment();
        System.out.println(operations.get());
    }

    // redis的事务管理：事务开始时，将数据命令放到队列里，等事务完毕后，一并执行，所以不能在事务中间进行数据访问，也因此，我们通常不会使用声明式事务，而是采用编程式事务
    @Test
    public void  testTranscational() {
        Object obj = redisTemplate.execute(new SessionCallback() {
            @Override
            public Object execute(RedisOperations operations) throws DataAccessException {
                String redisKey = "test:tx";
                operations.multi(); //启用事务
                operations.opsForSet().add(redisKey,"zhangsan");
                operations.opsForSet().add(redisKey, "xiaoming");
                operations.opsForSet().add(redisKey, "xiaohong");
                System.out.println(operations.opsForSet().members(redisKey));  // 只有提交的时候这三行命令才会被执行, 所以打印出来的结果会是空
                return operations.exec(); //提交事务
            }
        });
        System.out.println(obj);
    }
}
