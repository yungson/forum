package org.example.forum.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.core.search.TotalHits;
import co.elastic.clients.elasticsearch.core.search.TotalHitsRelation;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.ElasticsearchTransport;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.example.forum.dao.DiscussPostMapper;
import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class ElasticSearchService {
    private static final Logger logger = LoggerFactory.getLogger(ElasticSearchService.class);

    @Value("${es.host}")
    private String esHost;

    @Value("${es.port}")
    private Integer esPort;

    @Autowired
    private DiscussPostMapper discussPostMapper;

    private ElasticsearchClient client;

    @PostConstruct
    public void init() throws IOException {
        RestClient restClient = RestClient.builder(
                new HttpHost(esHost, 9200)).build();

        // Create the transport with a Jackson mapper
        ElasticsearchTransport transport = new RestClientTransport(
                restClient, new JacksonJsonpMapper());

        // And create the API client
        ElasticsearchClient client = new ElasticsearchClient(transport);
        BooleanResponse indexExist = client.indices().exists(r -> r.index("discusspost"));
        if (!indexExist.value()) {
            logger.info("index does not exist, creating index discusspost....");
            client.indices().create(r->r.index("discusspost"));
        }else {
            logger.info("index [discusspost] already exist, skip...");
        }
        int totalRows = discussPostMapper.selectDiscussPostRows(0);
        List<DiscussPost> posts = discussPostMapper.selectDiscussPosts(0, 0, totalRows, 0);
        logger.info("???????????????????????????"+posts.size());
        this.client = client;
        for (DiscussPost post : posts) {
            logger.info("saving : "+post);
            saveDiscussPost(post);
        }
    }
    public void saveDiscussPost(DiscussPost post) throws IOException {
        IndexResponse response = client.index(i -> i
                .index("discusspost")
                .id(String.valueOf(post.getId()))
                .document(post)
        );
    }

    public void deleteDiscussPost(int id) throws IOException {
        client.delete(i->i
                .index("discusspost")
                .id(String.valueOf(id))
        );
    }

    public SearchResult searchDiscussPost(String keyword, int current, int limit){
        SearchResponse<DiscussPost> searchResponse = null;
        try {
            searchResponse = client.search(s -> s
                            .index("discusspost")
                            .query(q->q
                                    .multiMatch(a->a
                                            .fields("title", "content")
                                            .query(keyword)
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
                            .from(current).size(limit),
                    DiscussPost.class);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ElasticsearchException e) {
            e.printStackTrace();
        }

        TotalHits total = searchResponse.hits().total();
        boolean isExactResult = total.relation() == TotalHitsRelation.Eq;

        if (isExactResult) {
            logger.info("There are " + total.value() + " results");
        } else {
            logger.info("There are more than " + total.value() + " results");
        }

        List<DiscussPost> list = new ArrayList<>();
        for (Hit<DiscussPost> hit: searchResponse.hits().hits()) {
//            {title=[<span style='color:red'>?????????</span>??????????????????],
//            content=[????????????????????????????????????????????????????????????60+??????????????????<em>?????????</em>???????????????????????????18???&19????????????0 offer???]}
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
        return new SearchResult(list, total.value());
    }
}


