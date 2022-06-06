package org.example.forum;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.example.forum.dao.DiscussPostMapper;
import org.example.forum.dao.elasticsearch.DiscussPostRepository;
import org.example.forum.entity.DiscussPost;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@SpringBootTest
@ContextConfiguration(classes = ForumApplication.class)
public class ElasticSearchTests {
    private static Logger logger = LoggerFactory.getLogger(ElasticSearchTests.class);

    @Autowired
    private DiscussPostMapper discussPostMapper;

    @Autowired
    private DiscussPostRepository discussPostRepository;

    // Create the low-level client
    RestClient restClient = RestClient.builder(
            new HttpHost("localhost", 9200)).build();

    // Create the transport with a Jackson mapper
    ElasticsearchTransport transport = new RestClientTransport(
            restClient, new JacksonJsonpMapper());

    // And create the API client
    ElasticsearchClient client = new ElasticsearchClient(transport);


//    @Test
//    public void testInsert() {
//        discussPostRepository.save(discussPostMapper.selectDiscussPostById(241));
//        discussPostRepository.save(discussPostMapper.selectDiscussPostById(242));
//        discussPostRepository.save(discussPostMapper.selectDiscussPostById(243));
//    }
//
//    @Test
//    public void testInsertList() {
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(101, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(102, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(103, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(111, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(112, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(131, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(132, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(133, 0, 100));
//        discussPostRepository.saveAll(discussPostMapper.selectDiscussPosts(134, 0, 100));
//    }


    @Test
    public void testBulkIndex() throws IOException {
        List<DiscussPost> posts = new ArrayList<>();
        List<Integer> ids = new ArrayList<>(Arrays.asList(101, 102, 103, 111, 112, 131, 132, 133, 134));
        BulkRequest.Builder br = new BulkRequest.Builder();
        for(Integer postId: ids){
            posts.addAll(discussPostMapper.selectDiscussPosts(postId, 0, 100));
        }
        for (DiscussPost post : posts) {
            br.operations(op -> op
                    .index(idx -> idx
                            .index("discusspost")
                            .id(String.valueOf(post.getId()))
                            .document(post)
                    )
            );
        }
        BulkResponse result = client.bulk(br.build());
        if (result.errors()) {
            logger.error("Bulk had errors");
            for (BulkResponseItem item: result.items()) {
                if (item.error() != null) {
                    logger.error(item.error().reason());
                }
            }
        }
    }
//    @Test
//    public void testUpdate() {
//        DiscussPost post = discussPostMapper.selectDiscussPostById(231);
//        post.setContent("我是新人，使劲灌水");
//        discussPostRepository.save(post);
//    }
//
//    @Test
//    public void testDelete() {
//        //discussPostRepository.deleteById(231);
//        discussPostRepository.deleteAll();
//    }


    @Test
    public void HighlightSearch() throws Exception {
        SearchResponse<DiscussPost> searchResponse = client.search(s -> s
                        .index("discusspost")
                        .query(q->q
                                .multiMatch(a->a
                                        .fields("title", "content")
                                        .query("届")
                        ))
                        .sort(f->f
                                .field(o->o
                                        .field("type")
                                        .order(SortOrder.Desc)))
                        .sort(f->f
                                .field(o->o
                                        .field("score")
                                        .order(SortOrder.Desc)))
                        .sort(f->f
                                .field(o->o
                                        .field("createTime")
                                        .order(SortOrder.Desc)))
                        .highlight(h->h
                                .fields("title",f->f
                                        .preTags("<span style='color:red'>")
                                        .postTags("</span>"))
                                .fields("content", f->f
                                        .preTags("<em>")
                                        .postTags("</em>")))
                        .from(0).size(10),
                DiscussPost.class);

        TotalHits total = searchResponse.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;
        if (isExactResult) {
            logger.info("There are " + total.value() + " results");
        } else {
            logger.info("There are more than " + total.value() + " results");
        }
        List<DiscussPost> list = new ArrayList<>();
        for (Hit<DiscussPost> hit: searchResponse.hits().hits()) {
//            {title=[<span style='color:red'>互联网</span>求职暖春计划],
//            content=[为了帮助大家度过“艰难”，牛客网特别联合60+家企业，开启<em>互联网</em>求职暖春计划，面向18届&19届，拯救0 offer！]}
            DiscussPost post = hit.source();
            System.out.println(hit.highlight());
            if(hit.highlight().get("title")!=null){
                post.setTitle(hit.highlight().get("title").get(0));
            }
            if(hit.highlight().get("content")!=null){
                post.setContent(hit.highlight().get("content").get(0));
            }
            list.add(post);
        }
        System.out.println(list.size());
    }
}

//    @Test
//    public void testSearch() {
//        NativeSearchQuery searchQuery = new NativeSearchQueryBuilder()
//                .withQuery(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                .withSorts(SortBuilders.fieldSort("type").order(SortOrder.DESC)) // 置顶
//                .withSorts(SortBuilders.fieldSort("score").order(SortOrder.DESC)) // 加精分数
//                .withSorts(SortBuilders.fieldSort("createTime").order(SortOrder.DESC)) // 时间
//                .withPageable(PageRequest.of(0, 10))
//                .withHighlightFields(
//                        new HighlightBuilder.Field("title").preTags("<em>").postTags("</em>"),
//                        new HighlightBuilder.Field("content").preTags("<em>").postTags("</em>")
//                ).build();
//    }

//        //高亮
//        HighlightBuilder highlightBuilder = new HighlightBuilder();
//        highlightBuilder.field("title");
//        highlightBuilder.field("content");
//        highlightBuilder.requireFieldMatch(false);
//        highlightBuilder.preTags("<span style='color:red'>");
//        highlightBuilder.postTags("</span>");
//
//        //构建搜索条件
//        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder()
//                .query(QueryBuilders.multiMatchQuery("互联网寒冬", "title", "content"))
//                .sort(SortBuilders.fieldSort("type").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("score").order(SortOrder.DESC))
//                .sort(SortBuilders.fieldSort("createTime").order(SortOrder.DESC))
//                .from(0)// 指定从哪条开始查询
//                .size(10)// 需要查出的总记录条数
//                .highlighter(highlightBuilder);//高亮
//
//        searchRequest.source(searchSourceBuilder);
//        SearchResponse searchResponse = client.search(s)
////                restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);

//        System.out.println(searchResponse);
//        List<DiscussPost> list = new LinkedList<>();
//        for (SearchHit hit : searchResponse.hits()) {
//
//        }
//            DiscussPost discussPost = JSONObject.parseObject()
//
//            // 处理高亮显示的结果
//            HighlightField titleField = (HighlightField) hit.getHighlightFields().get("title");
//            if (titleField != null) {
//                discussPost.setTitle(titleField.getFragments()[0].toString());
//            }
//            HighlightField contentField = hit.getHighlightFields().get("content");
//            if (contentField != null) {
//                discussPost.setContent(contentField.getFragments()[0].toString());
//            }
//            System.out.println(discussPost);
//            list.add(discussPost);
