package org.example.forum;

import org.example.forum.dao.AlphaDao;
import org.example.forum.service.AlphaService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebFlux;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;

import java.text.SimpleDateFormat;
import java.util.Date;

@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class) // 因为我们这里是测试类，我们想测试的时候用的类就是线上的那个类(给spring的配置类)， 所以此处通过这样的方式实现
class ForumApplicationTests implements ApplicationContextAware { // spring 容器是自动创建的，测试类如何得到这个容器？方法：任何类想要得到一个spring容器，都要写这行，implements ApplicationContextAware, 并实现里面的setApplicationContext方法

	private ApplicationContext applicationContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException { //
		// applicationContext其实就是一个spring 容器，源码里面applicationContext 是一个interface, 继承于HierarchicalBeanFactory, 继承于BeanFactory
		// BeanFactory是spring 容器的顶层interface. ApplicationContext 这个子接口比BeanFactory实现了更多的方法，功能更强，因此通常用此接口。如果一个类(bean)实现了ApplicationContext
		// 的setApplicationContext的方法, spring容器会检测到，并调用此接口，把自身传递进来。
		this.applicationContext = applicationContext; //我们使用此成员变量记录通过setApplicationContext传递进来的spring容器，就可以在测试类中使用了
	}

	@Test
	public void testApplicationContext(){
		System.out.println(applicationContext);
		AlphaDao alphaDao = applicationContext.getBean(AlphaDao.class); //从容器中获取某个类型的bean
		System.out.println(alphaDao.select()); // alphaDao是一个接口，可能有很多个实现类，比如之前一直某个项目一直用Hibernate去实现，现在要升级为MyBatis, 那么这样源代码就不用动
		// 我们重新在dao中新建一个AlphaDaoMyBatisImpl即可， 而此处的测试代码也不用改，这是面向接口的一种思想。当AlphaDao有多个实现类的时候，此处我们getBean是按类型获取的，这时已经有多个
		// AlphaDao的class了，就会报错。此时，可以在某一个实现类上加上@Primary注解解决。 比如程序的某一个地方需要一个特殊的实现类，可以通过bean的名字来实现（bean的名字默认是类名，首字母改成小写）
		// 还可以用将@Repository("alphaHibernate")这样的注解加到实现类上，来给实现类取别名名， 然后通过下面的代码获取
		alphaDao = applicationContext.getBean("alphaHibernate", AlphaDao.class);
		System.out.println(alphaDao.select());
	}

	@Test
	public void testBeanManagement() {
		/*
		* 会依次看到AlphaService.AlphaService，Initializing AlphaService， destroying AlphaService 说明spring正确的管理了bean初始化和destroy
		* 还可以看到Initializing AlphaService， destroying AlphaService之间隔了很多其他输出，这说明 spring管理的容器默认是只实例化一次，即单例模式
		* 如果想改成每次getBean都新建一个实例，需要在AlphaService上加上@Scope("prototype")注解， 但是通常都是单例模式:@Scope("singleton")。
		* 如果对别人写的第三方jar包里面的bean也想通过spring管理的话，由于我们不能改jar包去加注解，可以通过写配置类的方式实现, 参见config包
		* */
		AlphaService alphaService  = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);
		// 如果是@Scope("prototype") 就可以发现两次打印是不一样的hash value
		alphaService  = applicationContext.getBean(AlphaService.class);
		System.out.println(alphaService);
	}

	@Test
	public void testBeanConfig() {
		SimpleDateFormat simpleDateFormat = applicationContext.getBean(SimpleDateFormat.class);
		System.out.println(simpleDateFormat.format(new Date()));
	}

	//以上都是通过主动从spring容器中获取bean, 比较麻烦，spring更简单是通过依赖注入直接获取, @Autowired

	@Autowired
	@Qualifier("alphaHibernate") //标识将alphaHibernate实现的bean注入到这里
	private AlphaDao alphaDao;
	@Autowired
	private AlphaService alphaService;
	@Autowired
	private SimpleDateFormat simpleDateFormat;
	@Test
	public void testDI() {
		System.out.println(alphaDao.select());
		System.out.println(alphaService);
		System.out.println(simpleDateFormat);
	}
	// @Autowired也可以加在类的构造器或者set方法之前，但是加在属性前面更常用


}
