package org.example.forum;

import org.example.forum.util.SensitiveFilter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class SensitiveTests {
    @Autowired
    private SensitiveFilter sensitiveFilter;

    @Test
    public void testSensitiveFilter(){
        String text = "&lt;h1&gt;可以开票嫖娼哈哈哈&lt;/h1&gt;";
        text = sensitiveFilter.filter(text);
        System.out.println(text);
    }
}
