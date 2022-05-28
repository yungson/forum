package org.example.forum.service;

import org.example.forum.dao.AlphaDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Service
public class AlphaService {

    @Autowired
    private AlphaDao alphaDao;

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
}
