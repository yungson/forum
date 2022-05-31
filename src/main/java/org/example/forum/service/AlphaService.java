package org.example.forum.service;

import org.example.forum.dao.AlphaDao;
import org.example.forum.dao.DiscussPostMapper;
import org.example.forum.dao.UserMapper;
import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.User;
import org.example.forum.util.ForumUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.Date;

@Service
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    public AlphaService() {
        System.out.println("AlphaService.AlphaService");
    }

    @PostConstruct // spring容器可以帮助我们管理bean的初始化和销毁，让spring容器管理的本质就是在合适的地方加上合适的注解。PostConstruct就是在构造器之后调用
    public void init() {
        System.out.println("Initializing AlphaService");
    }

    @PreDestroy
    public void destroy() {
        System.out.println("destorying AlphaService");
    }

    public String find() {
        return alphaDao.select();
    }

    // Propagation: 表示事务传播机制。当该方法调用了其他方法b的时候，如果b上也有@Transcational事务管理，那么b的事务管理该以谁为准？传播机制就定义了这样的规则
    // REQUIRED: 支持当前事务，a调用b, 对于b来说，a是当前事务(说明b被分配了这个事务，这是b当前要处理的事情)，如果不存在则创建新事务
    // REQUIRES_NEW: 创建一个新事务，并且暂停当前事务。 a调用b,b不管a怎么样，永远创建新的事务
    // NESTED: 如果存在当前事务，则嵌套在该事务中执行(有独立的提交和回滚)。 比如a调用了b, 则b嵌套在a中执行，但是b也有独立的提交和回滚
    @Transactional(isolation = Isolation.READ_COMMITTED, propagation = Propagation.REQUIRED)
    public Object save1(){
        // 新增用户
        User user = new User();
        user.setUsername("alpha");
        user.setSalt(ForumUtil.generateUUID().substring(0,5));
        user.setPassword(ForumUtil.md5("123"+user.getSalt()));
        user.setEmail("alpha@qq.com");
        user.setHeaderUrl("http://image.nowcoder.com/head/99t.png");
        user.setCreateTime(new Date());
        userMapper.insertUser(user);
        // 新增帖子
        DiscussPost post = new DiscussPost();
        post.setUserId(user.getId());
        post.setTitle("hello");
        post.setContent("新人报道");
        post.setCreateTime(new Date());
        discussPostMapper.insertDiscussPost(post);

        // 人为制造报错过程，因为我们想看事务管理有没有效果，所以想在此让程序报错后看能不能回滚回去
        Integer.valueOf("abc");
        return "ok";
    }

    public Object save2(){
        transactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transactionTemplate.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRED);
        return transactionTemplate.execute(new TransactionCallback<Object>() { // 编程式事务需要把方法逻辑写到这个回调方法里去，然后transationTemplate会在底层自动执行，同时实现事务管理
            @Override
            public Object doInTransaction(TransactionStatus status) {
                // 新增用户
                User user = new User();
                user.setUsername("alpha1");
                user.setSalt(ForumUtil.generateUUID().substring(0,5));
                user.setPassword(ForumUtil.md5("1234"+user.getSalt()));
                user.setEmail("alpha1@qq.com");
                user.setHeaderUrl("http://image.nowcoder.com/head/999t.png");
                user.setCreateTime(new Date());
                userMapper.insertUser(user);
                // 新增帖子
                DiscussPost post = new DiscussPost();
                post.setUserId(user.getId());
                post.setTitle("hello1");
                post.setContent("新人报道1");
                post.setCreateTime(new Date());
                discussPostMapper.insertDiscussPost(post);

                // 人为制造报错过程，因为我们想看事务管理有没有效果，所以想在此让程序报错后看能不能回滚回去
                Integer.valueOf("abc");
                return null;
            }
        });

    }
}
