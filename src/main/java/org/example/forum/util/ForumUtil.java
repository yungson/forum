package org.example.forum.util;


import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.UUID;

public class ForumUtil {

    // generate a random string
    public static String generateUUID(){
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    //MD5 encryption. decryption of MD5 is impossible
    //Password +Salt 防止他人破解 to improve security
    public static String md5(String key){
        if(StringUtils.isBlank(key)) {
            return null;
        }
        return DigestUtils.md5DigestAsHex(key.getBytes());
    }

}
