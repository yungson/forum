package org.example.forum.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.forum.entity.DiscussPost;

import java.util.List;

@Mapper
public interface DiscussPostMapper {
    /*
    * offset：line number of the staring line
    * limit: max line */
    List<DiscussPost> selectDiscussPosts(int userId, int offset, int limit, int orderMode); //如果是首页，那么userId=0

    /* 如果方法只有一个参数，并且在动态sql<if>里使用，则必须用@Param取别名, 否则后面sql里会报错
    * */
    int selectDiscussPostRows(@Param("userId") int userId);
    int insertDiscussPost(DiscussPost discussPost);
    DiscussPost selectDiscussPostById(int id);
    int insertComment();
    int updateCommentCount(int id, int commentCount);

    int updateType(int id, int type);
    int updateStatus(int id, int status);

    int updateScore(int id, double score);
}
