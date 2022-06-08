package org.example.forum.event;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.Event;
import org.example.forum.entity.Message;
import org.example.forum.service.DiscussPostService;
import org.example.forum.service.ElasticSearchService;
import org.example.forum.service.MessageService;
import org.example.forum.util.ForumConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Component
public class EventConsumer implements ForumConstant {

    private static final Logger logger = LoggerFactory.getLogger(EventConsumer.class);

    @Autowired
    private MessageService messageService;

    @Autowired
    private DiscussPostService discussPostService;

    @Autowired
    private ElasticSearchService elasticSearchService;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Value("${wk.image.command}")
    private String wkCommand;

    // 一个方法可以消费多个书体
    // 一个主题也可以被多个方法消费
    @KafkaListener(topics = {TOPIC_COMMENT, TOPIC_LIKE, TOPIC_FOLLOW})
    public void handleCommentMessage(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("Event can not be null");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if( event == null){
            logger.error("Incorrect message format from producer!");
            return;
        }
        Message message = new Message();
        message.setFromId(SYSTEM_USER_ID);
        message.setToId(event.getEntityUserId());
        message.setConversationId(event.getTopic());
        // status defaults to 0, no need to set
        message.setCreateTime(new Date());

        Map<String, Object> content = new HashMap<>();
        content.put("userId", event.getUserId());
        content.put("entityType", event.getEntityType());
        content.put("entityId", event.getEntityId());
        if(!event.getData().isEmpty()){
            for (Map.Entry<String, Object> entry: event.getData().entrySet()){
                content.put(entry.getKey(), entry.getValue());
            }
        }
        message.setContent(JSONObject.toJSONString(content));
        messageService.addMessage(message);
    }

    // 消费发帖事件
    @KafkaListener(topics = {TOPIC_PUBLISH})
    public void handlePublishMessage(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("Event can not be null");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if( event == null){
            logger.error("Incorrect message format from producer!");
            return;
        }
        DiscussPost post = discussPostService.findDiscussPostById(event.getEntityId());
        try {
            elasticSearchService.saveDiscussPost(post);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = {TOPIC_DELETE})
    public void handleDeleteMessage(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("Event can not be null");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if( event == null){
            logger.error("Incorrect message format from producer!");
            return;
        }
        try {
            elasticSearchService.deleteDiscussPost(event.getEntityId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @KafkaListener(topics = {TOPIC_SHARE})
    public void handleShareMessage(ConsumerRecord record){
        if (record == null || record.value() == null) {
            logger.error("Event can not be null");
            return;
        }
        Event event = JSONObject.parseObject(record.value().toString(), Event.class);
        if( event == null){
            logger.error("Incorrect message format from producer!");
            return;
        }
        String htmlUrl = (String) event.getData().get("htmlUrl");
        String fileName = (String)  event.getData().get("fileName");
        String suffix = (String)  event.getData().get("suffix");
        String cmd = wkCommand+" --quality 75 "+htmlUrl+" "+wkImageStorage+"/"+fileName+suffix;
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            if (p.waitFor() == 0 ){
                logger.info("成功生成长图!");
            }
        } catch (IOException e) {
            logger.error("Fail to generate share png: "+e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
