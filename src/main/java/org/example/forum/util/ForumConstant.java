package org.example.forum.util;

public interface ForumConstant {

    int ACTIVATION_SUCCESS = 0;
    int ACTIVATION_REPEAT = 1;
    int ACTIVATION_FAILURE = 2;

    int DEFAULT_EXPIRED_SECONDS = 3600*12; //default expired seconds for login_ticket
    int REMEMBER_EXPIRED_SECONDS = 3600*24*100;
}