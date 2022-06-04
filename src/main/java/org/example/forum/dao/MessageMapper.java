package org.example.forum.dao;

import org.apache.ibatis.annotations.Mapper;
import org.example.forum.entity.Message;

import java.util.List;

@Mapper
public interface MessageMapper {
    // 查询当前用户的会话列表，针对每个会话只返回一条最新的私信
    List<Message> selectConversations(int userId, int offset, int limit);

    // 查询当前用户的会话数量
    int selectConversationCount(int userId);

    // 查询某个会话所包含的私信列表
    List<Message> selectLetters(String conversationId, int offset, int limit);

    //查询某个会话所包含的私信数量
    int selectLetterCount(String conversationId);

    // 查询未读私信的数量
    int selectUnreadLetterCount(int userId, String conversationId);

    // 新增消息
    int insertMessage(Message message);

    // 修改消息的状态
    int updateStatus(List<Integer> ids, int status);

    // select the latest one notice from specified topic
    Message selectLatestNotice(int userId, String topic);

    int selectNoticeCount(int userId, String topic);

    int selectUnreadNoticeCount(int userId, String topic);

    List<Message> selectNotices(int userId, String topic, int offset, int limit);

}
