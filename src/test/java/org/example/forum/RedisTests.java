package org.example.forum;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisStringCommands;
import org.springframework.data.redis.core.*;
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

    // 演示HyperLogLog的使用，统计20万个重复数据的独立总数
    @Test
    public void testHyperLogLog() {
        String redisKey = "test:hll:01";
        for (int i=1;i <=100000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey, i);
        }
        for (int i=1;i <=100000;i++){
            int r = (int) (Math.random()*100000+1); //Math.random() 是[0,1) ， Math.random()*100000+1是[1,100001), 取整就是[1,100000]
            redisTemplate.opsForHyperLogLog().add(redisKey, r);
        }

        // 理论上去重后应该是100000
        long size = redisTemplate.opsForHyperLogLog().size(redisKey);
        System.out.println(size); //99562
    }

    // 将3组数据合并，再统计合并后的重复数据的独立总数
    // 常见应用与从每天的UV合并得到周UV，月UV
    @Test
    public void testHyperLogLogUnion() {
        String redisKey2 = "test:hll:02";
        for (int i=1;i <=10000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey2, i);
        }
        String redisKey3 = "test:hll:03";
        for (int i=5001;i <=15000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey3, i);
        }
        String redisKey4 = "test:hll:04";
        for (int i=10001;i <=20000;i++){
            redisTemplate.opsForHyperLogLog().add(redisKey4, i);
        }

        String redisKeyUnion = "test:hll:union";
        redisTemplate.opsForHyperLogLog().union(redisKeyUnion,redisKey2,redisKey3,redisKey4);
        // 理论上去重后应该是20000
        long size = redisTemplate.opsForHyperLogLog().size(redisKeyUnion);
        System.out.println(size); //19891
    }


    // 统计一组数据的布尔值
    @Test
    public void testBitMap() {
        String redisKey = "test:bm:01";

        redisTemplate.opsForValue().setBit(redisKey, 1, true);
        redisTemplate.opsForValue().setBit(redisKey, 4, true);
        redisTemplate.opsForValue().setBit(redisKey, 7, true);

        System.out.println(redisTemplate.opsForValue().getBit(redisKey,1));
        System.out.println(redisTemplate.opsForValue().getBit(redisKey,4));
        // 统计

        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj); // 3


    }

    // 统计3组数据的布尔值，并对3组数据做OR运算

    @Test
    public void testBitMapOp() {
        String redisKey2 = "test:bm:02";

        redisTemplate.opsForValue().setBit(redisKey2, 0, true);
        redisTemplate.opsForValue().setBit(redisKey2, 1, true);
        redisTemplate.opsForValue().setBit(redisKey2, 2, true);

        String redisKey3 = "test:bm:03";

        redisTemplate.opsForValue().setBit(redisKey3, 2, true);
        redisTemplate.opsForValue().setBit(redisKey3, 3, true);
        redisTemplate.opsForValue().setBit(redisKey3, 4, true);

        String redisKey4 = "test:bm:04";

        redisTemplate.opsForValue().setBit(redisKey4, 4, true);
        redisTemplate.opsForValue().setBit(redisKey4, 5, true);
        redisTemplate.opsForValue().setBit(redisKey4, 6, true);

        String redisKey = "test:bm:or";
        Object obj = redisTemplate.execute(new RedisCallback() {
            @Override
            public Object doInRedis(RedisConnection connection) throws DataAccessException {
                connection.bitOp(RedisStringCommands.BitOperation.OR, redisKey.getBytes(), redisKey2.getBytes(), redisKey3.getBytes(), redisKey4.getBytes());
                return connection.bitCount(redisKey.getBytes());
            }
        });
        System.out.println(obj); // 7
    }

}
