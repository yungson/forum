package org.example.forum.dao;

import org.apache.ibatis.annotations.*;
import org.example.forum.entity.LoginTicket;

@Mapper
public interface LoginTicketMapper {
    @Insert({
            "insert into login_ticket(user_id, ticket, status, expired) ",
            "values(#{userId}, #{ticket}, #{status}, #{expired})"
    })
    @Options(useGeneratedKeys = true, keyProperty = "id") // Options注解给上面的Insert语句 useGeneratedKeys表示插入的时候自动生成主键，keyProperty表示生成后将id自动注入给loginTicket对象
    int insertLoginTicket(LoginTicket loginTicket);

    @Select({
            "select id, user_id, ticket, status, expired ",
            "from login_ticket where ticket=#{ticket}"
    })
    LoginTicket selectByTicket(String ticket);


    @Update({
            "<script> ",
            "update login_ticket set status=#{status} where ticket=#{ticket} ",
            "<if test=\"ticket!=null\"> ",
            "and 1=1 ",
            "</if> ",
            "</script>"
    })
    int updateStatus(String ticket, int status);
}

//实现与数据库的交互通常有两种方式，一种是通过在resources/mapper下面配置对应的配置文件
// 另外一种就是通过注解将Insert Select等语句 annotate 到对应方法上

// 在注解里写sql的时候也支持动态的sql, 参考上面的Update