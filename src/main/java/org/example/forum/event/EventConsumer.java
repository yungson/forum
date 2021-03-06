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
import java.util.concurrent.TimeUnit;

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

    // ????????????????????????????????????
    // ??????????????????????????????????????????
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

    // ??????????????????
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
            // ???????????????????????????ProcessBuilder?????????????????????????????????wkCommand+args???????????????
            // ????????????File Not found???????????????java???process????????????"wkCommand+args"??????
//            Process p = new ProcessBuilder(wkCommand, args).start();
//            p.waitFor(30000, TimeUnit.MILLISECONDS);
            Process p = Runtime.getRuntime().exec(cmd);
            // ???????????????????????????????????????????????????waitFor, sub-process??????????????????, ???????????????
            p.waitFor(30000, TimeUnit.MILLISECONDS);
        } catch (IOException e) {
            logger.error("Fail to generate share png: "+e.getMessage());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        // ????????????????????????share?????????????????????????????????????????????
        // ???????????????????????????quartz?????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????????
        UploadTask uploadTask = new UploadTask(fileName, suffix);
        Future future = threadPoolTaskScheduler.scheduleAtFixedRate(uploadTask,500);
        uploadTask.setFuture(future);
    }

    class UploadTask implements  Runnable{
        private String fileName;
        private String suffix;
        private Future future; // ????????????????????????
        private long startTime; //????????????
        private int uploadTimes; // ????????????
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
            // ????????????
            if(System.currentTimeMillis()-startTime>30000){
                logger.error("??????????????????????????????????????????"+fileName+suffix);
                future.cancel(true);
                return;
            }
            if(uploadTimes>=3){
                logger.error("??????????????????????????????????????????"+fileName+suffix);
                future.cancel(true);
                return;
            }
            String path = wkImageStorage+"/"+fileName+suffix;
            File file = new File(path);
            if (file.exists()){
                logger.info(String.format("?????????[%d]?????????[%s]",++uploadTimes, fileName+suffix));
                // ??????????????????
                StringMap policy = new StringMap();
                policy.put("returnBody", ForumUtil.getJSONString(0));
                Auth auth = Auth.create(accessKey, accessSecret);
                String uploadToken = auth.uploadToken(shareName, fileName+suffix, 3600, policy);
                UploadManager manager = new UploadManager(new Configuration(Region.region1()));
                try{
                    logger.info(path);
                    Response response = manager.put(path, fileName+suffix, uploadToken, null, "image/png", false);
                    JSONObject json = JSONObject.parseObject(response.bodyString());
                    if (json == null || json.get("code") == null || !json.get("code").toString().equals("0")) {
                        logger.info(String.format("???[%d]???????????????[%s]", uploadTimes, fileName+suffix));
                    } else {
                        logger.info(String.format("???[%d]???????????????[%s]", uploadTimes, fileName+suffix));
                        future.cancel(true);
                    }
                } catch (QiniuException e){
                    logger.info(String.format("???[%d]???????????????[%s], error:[%s]", uploadTimes, fileName+suffix, e.getMessage()));
                }
            }else{
                logger.info("??????????????????..."+fileName+suffix);
            }
        }
    }
}
