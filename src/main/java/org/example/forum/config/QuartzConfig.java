package org.example.forum.config;

import org.example.forum.quartz.AlphaJob;
import org.example.forum.quartz.PostScoreRefreshJob;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.JobDetailFactoryBean;
import org.springframework.scheduling.quartz.SimpleTriggerFactoryBean;

// 配置-》数据库-〉用于之后调用
@Configuration
public class QuartzConfig {

    // 之前的BeanFactory是整个IoC容器的顶层接口
    // FactoryBean可简化bean的实例化过程；因为有些Bean的实例化过程比较麻烦。可以理解为JobDetailFactoryBean定义了Bean详细的实例化过程
    // 1。 通过FactoryBean封装Bean的实例化过程
    // 2。 将FactoryBean装配到Spring容器里
    // 3。 将FactoryBean注入给其他的Bean
    // 4。 该Bean得到的是FactoryBean所管理的对象实例


    // 配置JobDetail
//    @Bean //暂时注释掉，因为是演示脚本，如果要用还需要再加回来
    public JobDetailFactoryBean alphaJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(AlphaJob.class);
        factoryBean.setName("alphaJob");
        factoryBean.setGroup("alphaJobGroup");
        factoryBean.setDurability(true); //是否长久保存
        factoryBean.setRequestsRecovery(true); //任务是否可恢复
        return factoryBean;
    }

    // 配置Trigger（SimpleTriggerFactoryBean(用于简单的)， CronTriggerFactoryBean（用于复杂的定时的））
//    @Bean //暂时注释掉，因为是演示脚本，如果要用还需要再加回来
    public SimpleTriggerFactoryBean alphaTrigger(JobDetail alphaJobDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(alphaJobDetail);
        factoryBean.setName("alphaTrigger");
        factoryBean.setGroup("alphaTriggerGroup");
        factoryBean.setRepeatInterval(3000);
        factoryBean.setJobDataMap(new JobDataMap()); // trigger底层用于存储job状态的datamap
        return factoryBean;
    }

    // 刷新帖子分数的任务
    @Bean
    public JobDetailFactoryBean postScoreRefreshJobDetail() {
        JobDetailFactoryBean factoryBean = new JobDetailFactoryBean();
        factoryBean.setJobClass(PostScoreRefreshJob.class);
        factoryBean.setName("postScoreRefreshJob");
        factoryBean.setGroup("forumJobGroup");
        factoryBean.setDurability(true); //是否长久保存
        factoryBean.setRequestsRecovery(true); //任务是否可恢复
        return factoryBean;
    }

    @Bean
    public SimpleTriggerFactoryBean postScoreRefreshTrigger(JobDetail postScoreRefreshJobDetail){
        SimpleTriggerFactoryBean factoryBean = new SimpleTriggerFactoryBean();
        factoryBean.setJobDetail(postScoreRefreshJobDetail);
        factoryBean.setName("postScoreRefreshTrigger");
        factoryBean.setGroup("forumTriggerGroup");
        factoryBean.setRepeatInterval(1000*60*5);
        factoryBean.setJobDataMap(new JobDataMap()); // trigger底层用于存储job状态的datamap
        return factoryBean;
    }
}
