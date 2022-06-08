package org.example.forum.util;

public interface ForumConstant {

    int ACTIVATION_SUCCESS = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;

    int DEFAULT_EXPIRED_SECONDS = 3600*12; //default expired seconds for login_ticket
    int REMEMBER_EXPIRED_SECONDS = 3600*24*100;

    int ENTITY_TYPE_POST = 1;
    int ENTITY_TYPE_COMMENT = 2;
    int ENTITY_TYPE_USER = 3;

    int SYSTEM_USER_ID = 1;

    String TOPIC_PUBLISH = "publish";
    String TOPIC_DELETE = "delete";

    String TOPIC_COMMENT = "comment";
    String TOPIC_LIKE = "like";
    String TOPIC_FOLLOW = "follow";
    String AUTHORITY_USER = "user"; // 普通用户
    String AUTHORITY_ADMIN = "admin"; // 管理员
    String AUTHORITY_MODERATOR = "moderator"; //版主
}
