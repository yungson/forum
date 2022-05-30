package org.example.forum.util;

import org.example.forum.entity.User;
import org.springframework.stereotype.Component;

/*
* HostHolder是用来持有用户信息，在拦截器场景下代替session对象的（session对象是可以直接存储用户信息的，且是直接线程隔离存储的所以不需要一个Holder来实现线程隔离的持有用户信息
* ， 但是我们现在想要存储拦截器的查询到的用户信息，所以需要用HostHolder来实现线程隔离存储
* threadLocal是如何线程隔离存储信息的？可以看源码，源码里有set方法和get方法，必须下面的set方法
*     public void set(T value) {
        Thread t = Thread.currentThread();
        ThreadLocalMap map = getMap(t);
        if (map != null) {
            map.set(this, value);
        } else {
            createMap(t, value);
        }
    }
    * 可以看到set方法是先获取当前线程，然后以线程为key 再往里面存值的
    * get是同理的*/
@Component
public class HostHolder {

    private ThreadLocal<User> users = new ThreadLocal<>();

    public void setUser(User user){
        users.set(user);
    }
    public User getUser(){
        return users.get();
    }
    public void clear(){
        users.remove();
    }
}
