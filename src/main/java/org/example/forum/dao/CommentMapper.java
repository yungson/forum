package org.example.forum.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.forum.entity.Comment;

import java.util.List;

@Mapper
public interface CommentMapper {
    List<Comment> selectCommentsByEntity(int entityType, int entityId, int offset, int limit);
    int selectCountByEntity(int entityType, int entityId); //查询comment的条目数量
    int insertComment(Comment comment);
    Comment selectCommentById(int id);
}
