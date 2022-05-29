package org.example.forum.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.forum.entity.User;


/*
* 创建一个UserMapper用于访问User entity, 因为底层都封装好了，所以只需要我们写接口，不需要写实现类
* 而且通常我们加@Repository来让spring装配这个bean. 但此时我们一般用mybatis的注解Mapper来标注，
* 当然repository仍然还是可以的
* */
@Mapper
public interface UserMapper {

    User selectById(int id);
    User selectByName(String username);
    User selectByEmail(String email);
    int insertUser(User user);
    int updateStatus(int id, int status);
    int updateHeader(int id, String headerUrl);
    int updatePassword(int id, String password);
    // 在声明这些方法之后，我们还需要写一个配置文件，给每一个方法提供它对应的sql, 这样mybatis底层会自动帮我们生成对应的实现类， 配置文件放在resources/mapper
}
