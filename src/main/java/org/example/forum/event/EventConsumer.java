package org.example.forum.event;

import com.alibaba.fastjson.JSONObject;
import com.qiniu.common.QiniuException;
import com.qiniu.http.Response;
import com.qiniu.storage.Configuration;
import com.qiniu.storage.Region;
import com.qiniu.storage.UploadManager;
import com.qiniu.util.Auth;
import com.qiniu.util.StringMap;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.example.forum.entity.DiscussPost;
import org.example.forum.entity.Event;
import org.example.forum.entity.Message;
import org.example.forum.service.DiscussPostService;
import org.example.forum.service.ElasticSearchService;
import org.example.forum.service.MessageService;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

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

    @Value("${qiniu.key.access}")
    private String accessKey;
    @Value("${qiniu.key.secret}")
    private String accessSecret;
    @Value("${qiniu.bucket.share.name}")
    private String shareName;
    @Autowired
    private ThreadPoolTaskScheduler threadPoolTaskScheduler;

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
        // 启用定时器，监视share的图片，一旦生成完毕上传七牛云
        // 为什么这个可以不用quartz？因为这个是消息队列，即使是分布式部署，某个服务器抢到消费事件时，其他服务器就拿不到这个事件了
        UploadTask uploadTask = new UploadTask(fileName, suffix);
        Future future = threadPoolTaskScheduler.scheduleAtFixedRate(uploadTask,500);
        uploadTask.setFuture(future);
    }

    class UploadTask implements  Runnable{
        private String fileName;
        private String suffix;
        private Future future; // 启动任务的返回值
        private long startTime; //开始时间
        private int uploadTimes; // 上传次数
        public UploadTask(String fileName, String suffix){
            this.fileName = fileName;
            this.suffix = suffix;
            this.startTime = System.currentTimeMillis();
        }

        public void setFuture(Future future) {
            this.future = future;
        }

        @Override
        public void run() {
            // 生成失败
            if(System.currentTimeMillis()-startTime>30000){
                logger.error("生成图片时间过长，终止任务："+fileName);
                future.cancel(true);
                return;
            }
            if(uploadTimes>=3){
                logger.error("上传图片次数过多，终止任务："+fileName);
                future.cancel(true);
                return;
            }
            String path = wkImageStorage+"/"+fileName+suffix;
            File file = new File(path);
            if (file.exists()){
                logger.info(String.format("开始第[%d]次上传[%s]",++uploadTimes,fileName));
                // 设置响应信息
                StringMap policy = new StringMap();
                policy.put("returnBody", ForumUtil.getJSONString(0));
                Auth auth = Auth.create(accessKey, accessSecret);
                String uploadToken = auth.uploadToken(shareName, fileName, 3600, policy);
                UploadManager manager = new UploadManager(new Configuration(Region.region1()));
                try{
                    logger.info(path);
                    Response response = manager.put(path, fileName, uploadToken, null, "image/"+suffix, false);
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("第[%d]次上传失败[%s]", uploadTimes, fileName));
                    } else {
                        logger.info(String.format("第[%d]次上传成功[%s]", uploadTimes, fileName));
                        future.cancel(true);
                    }
                } catch (QiniuException e){
                    logger.info(String.format("第[%d]次上传失败[%s], error:[%s]", uploadTimes, fileName, e.getMessage()));
                }
            }else{
                logger.info("等待图片生成..."+fileName);
            }
        }
    }
}
