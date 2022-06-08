package org.example.forum;

import org.example.forum.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.context.ContextConfiguration;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class ThreadPoolTests {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolTests.class);

    // JDK普通线程池
    private ExecutorService executorService = Executors.newFixedThreadPool(5);

    // JDK 可执行定时任务的线程池
    private ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(5);



    // spring的线程池不用我们实例化，spring自己会实例化，我们只需要inject
    @Autowired
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

    @Autowired
    private AlphaService alphaService;
    // 封装一个sleep方法以便后面测试用，因为test执行完，主线程就会退出
    private void sleep(long m){
        try {
            Thread.sleep(m);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void TestExecutorService(){

        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("hello");
            }
        };
        for (int i = 0;i<10;i++){
            executorService.submit(task);
        }
        sleep(10000);
    }

    @Test
    public void TestScheduledExecutorService(){

        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("scheduler");
            }
        };
        scheduledExecutorService.scheduleAtFixedRate(task, 10000, 1000, TimeUnit.MILLISECONDS);
        sleep(30000); // 阻塞一会，30000ms后就会退出否则定时任务会一直执行下去
    }


    @Test
    public void TestThreadPoolTaskExecutor(){
        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("TestThreadPoolTaskExecutor");
            }
        };
        for (int i = 0;i<10;i++){
            threadPoolTaskExecutor.submit(task);
        }
        sleep(10000);
    }

    @Test
    public void TestThreadPoolTaskScheduler(){

        Runnable task = new Runnable() {
            @Override
            public void run() {
                logger.debug("TestThreadPoolTaskScheduler");
            }
        };
        Date startTime = new Date(System.currentTimeMillis()+10000);
        threadPoolTaskScheduler.scheduleAtFixedRate(task, startTime, 1000);
        sleep(30000); // 阻塞一会，30000ms后就会退出否则定时任务会一直执行下去
    }


//    @Test
//    public void TestThreadPoolTaskExecutorSimple(){
//        for (int i = 0;i<10;i++){
//            alphaService.run1();
//        }
//        sleep(10000);
//    }
//
//    @Test
//    public void TestThreadPoolTaskSchedulerSimple(){
//        // 此处什么都不用写，alphaService的run2方法已经做了定时任务线程池的配置，他们会自动执行
//        // 此程序主要是为了让程序启动起来看输出
//        sleep(30000); // 阻塞一会，30000ms后就会退出否则定时任务会一直执行下去
//    }

}
