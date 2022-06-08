package org.example.forum.controller;

import org.apache.commons.lang3.StringUtils;
import org.example.forum.entity.Event;
import org.example.forum.event.EventProducer;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

@Controller
public class ShareController  implements ForumConstant  {
    private static final Logger logger = LoggerFactory.getLogger(ShareController.class);

    @Value("${forum.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Value("${wk.image.storage}")
    private String wkImageStorage;

    @Autowired
    private EventProducer eventProducer;

    @RequestMapping(path = "/share", method= RequestMethod.GET)
    @ResponseBody
    public String share(String htmlUrl){
        String fileName = ForumUtil.generateUUID();
        // 异步生成长图
        Event event = new Event()
                .setTopic(TOPIC_SHARE )
                .setData("htmlUrl", htmlUrl)
                .setData("fileName", fileName)
                .setData("suffix", ".png");
        eventProducer.fireEvent(event);
        Map<String, Object> map = new HashMap<>();
        map.put("shareUrl", domain+contextPath+"/share/image/"+fileName);
        return ForumUtil.getJSONString(0, null, map);
    }


    @RequestMapping(path = "/share/image/{fileName}", method= RequestMethod.GET)
    public void getShareImage(@PathVariable("fileName") String fileName, HttpServletResponse response){
        if (StringUtils.isBlank(fileName)){
            throw  new IllegalArgumentException("parameters can not be null");
        }

        response.setContentType("image/png");
        File file = new File(wkImageStorage+"/"+fileName+".png");
        try {
            OutputStream os = response.getOutputStream();
            FileInputStream fis = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            int b = 0;
            while ((b = fis.read(buffer)) != -1){
                os.write(buffer, 0, b);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
