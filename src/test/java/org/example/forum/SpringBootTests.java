package org.example.forum;

import org.example.forum.entity.DiscussPost;
import org.example.forum.service.DiscussPostService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;



@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class SpringBootTests {

    @Autowired
    private DiscussPostService discussPostService;

    private DiscussPost data;

    @BeforeAll // 在类初始化之前，静态的，只执行一次
    public static void beforeClass() {
        System.out.println("beforeClass");
    }

    @AfterAll
    public static void afterClass() {
        System.out.println("afterClass");
    }

    @Test
    public void test1() {
        System.out.println("test1");
    }

    @Test
    public void test2() {
        System.out.println("test2");
    }

    @BeforeEach// 在测试方法调用之前，每个实例每次方法调用都会执行一次
    public void before() {
        // 初始化测试数据
        System.out.println("before");
        data = new DiscussPost();
        data.setUserId(111);
        data.setTitle("test title");
        data.setContent("test content");
        data.setCreateTime(new Date());
        data.setScore(Math.random()*2000);
        int rows = discussPostService.addDiscussPost(data);
        System.out.println("the id is "+data.getId());
    }
    @AfterEach
    public void after() {
        // 删除测试数据
        System.out.println("after");
        discussPostService.updateStatus(data.getId(), 2);
    }

    @Test
    public void testFindById() {
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assertions.assertNotNull(post);
        Assertions.assertEquals(data.getContent(), post.getContent());
        Assertions.assertEquals(data.getTitle(), post.getTitle());
    }

    // 注意testFindById， testUpdateScore测试方法用的虽然都叫data， 但是他们其实是不同的数据，每个方法都重新初始化测试数据了
    @Test
    public void testUpdateScore() {
        int rows = discussPostService.updateScore(data.getId(), 2230.00);
        Assertions.assertEquals(rows, 1);
        DiscussPost post = discussPostService.findDiscussPostById(data.getId());
        Assertions.assertEquals(2230.00, post.getScore(), 2); // 2表示精度为2位小数
    }
}
