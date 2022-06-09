package org.example.forum.actuator;

import org.example.forum.util.ForumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

@Component
@Endpoint(id="database")
public class DatabaseEndpoint {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseEndpoint.class);

    @Autowired
    private DataSource dataSource;

    @ReadOperation
    public String checkConnection() {
        try(
                Connection conn = dataSource.getConnection();
        ) {
            return ForumUtil.getJSONString(0 , "获取数据库连接池正常");
        }catch (SQLException e){
            logger.error("获取数据库连接池失败："+e.getMessage());
            return ForumUtil.getJSONString(1,"获取数据库连接池异常");
        }
    }

}
