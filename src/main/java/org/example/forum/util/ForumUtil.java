package org.example.forum.util;


import com.alibaba.fastjson2.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.springframework.util.DigestUtils;

import java.util.HashMap;
import java.util.Map;
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

    public static String getJSONString(int code, String msg, Map<String, Object> map){
        JSONObject json = new JSONObject();
        json.put("code", code);
        json.put("msg", msg);
        if(map!=null){
            for(String key: map.keySet()){
                json.put(key, map.get(key));
            }
        }

        return json.toJSONString();
    }

    public static String getJSONString(int code, String msg){
        return getJSONString(code , msg, null);
    }
    public static String getJSONString(int code){
        return getJSONString(code, null, null);
    }

    public static void main(String[] args){
        Map<String, Object> map = new HashMap<>();
        map.put("name", "zhangsan");
        map.put("age", 25);
        System.out.println(getJSONString(0, "ok", map));
    }

}
