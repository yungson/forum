package org.example.forum.util;

public class RedisKeyUtil {

    private static final String SPLIT = ":";
    private static final String PREFIX_ENTITY_LIKE = "like:entity";
    private static final String PREFIX_USER_LIKE = "like:user";
    private static final String PREFIX_FOLLOWER = "follower";
    private static final String PREFIX_FOLLOWEE = "followee";
    private static final String PREFIX_KAPTCHA = "kaptcha";
    private static final String PREFIX_TICKET = "ticket";
    private static final String PREFIX_USER = "user";
    private static final String PREFIX_UV = "uv";
    private static final String PREFIX_DAU = "dau";
    private static final String PREFIX_POST = "post";

    //某个实体的赞
    public static String getEntityLikeKey(int entityType, int entityId){
        return PREFIX_ENTITY_LIKE+SPLIT+entityType+SPLIT+entityId;
    }
    //某个用户的赞
    public static String getUserLikeKey(int userId){
        return PREFIX_USER_LIKE+SPLIT+userId;
    }

    // 某个用户关注的实体
    // followee:userId:entityType -> zset(entityId, now)
    public static String getFolloweeKey(int userId, int entityType){
        return PREFIX_FOLLOWEE+SPLIT+userId+SPLIT+entityType;
    }

    //某个实体拥有的粉丝
    public static String getFollowerKey(int entityType, int entityId){
        return PREFIX_FOLLOWER+SPLIT+entityType+SPLIT+entityId;
    }

    public static String getKaptchaKey(String owner ){ // owner是用户的一个临时的凭证
        return PREFIX_KAPTCHA+SPLIT+owner;
    }

    public static String getTicketKey(String ticket){
        return PREFIX_TICKET+SPLIT+ticket;
    }
    public static String getUserKey(int userId){
        return PREFIX_USER+SPLIT+userId;
    }

    public static String getUVKey(String date){ // UV for a single day
        return PREFIX_UV + SPLIT+date;
    }
    public static String getUVKey(String startDate, String endDate){
        // UV for an interval
        return PREFIX_UV+SPLIT+startDate+SPLIT+endDate;
    }

    public static String getDAUKey(String date){ // DAU for a single day
        return PREFIX_DAU + SPLIT+date;
    }
    public static String getDAUKey(String startDate, String endDate){
        // DAU for an interval
        return PREFIX_DAU+SPLIT+startDate+SPLIT+endDate;
    }

    public static String getPostScoreKey(){
        return PREFIX_POST+SPLIT+"score";
    }
}
