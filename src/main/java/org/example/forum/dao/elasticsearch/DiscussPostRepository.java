package org.example.forum.dao.elasticsearch;

import org.example.forum.entity.DiscussPost;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DiscussPostRepository extends ElasticsearchRepository<DiscussPost, Integer> {
    //ElasticsearchRepository<DiscussPost, Integer> DiscussPost实体类的类型，Integer是实体类的组件类型？
    // spring会自动对此接口进行一个实现， 可以直接使用


}
