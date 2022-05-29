package org.example.forum.service;

import org.example.forum.dao.DiscussPostMapper;
import org.example.forum.entity.DiscussPost;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;


/*
* 无论service方法多简单，都要通过Controller调Service， Service调Mapper/DAO， 不要跳过，开发的可维护性和以后可能的升级，规范等 */
@Service
public class DiscussPostService {

    @Autowired
    private DiscussPostMapper discussPostMapper;

    public List<DiscussPost> findDiscussPosts(int userId, int offset, int limit){
        return discussPostMapper.selectDiscussPosts(userId, offset, limit);
    }

    public int findDiscussPostRows(int userId){
        return discussPostMapper.selectDiscussPostRows(userId);
    }


}
