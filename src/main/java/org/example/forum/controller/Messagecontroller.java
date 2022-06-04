package org.example.forum.controller;

import com.alibaba.fastjson.JSONObject;
import org.example.forum.entity.Message;
import org.example.forum.entity.Page;
import org.example.forum.entity.User;
import org.example.forum.service.MessageService;
import org.example.forum.service.UserService;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.example.forum.util.HostHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.util.HtmlUtils;

import java.util.*;

@Controller
public class Messagecontroller implements ForumConstant {
    @Autowired
    private MessageService messageService;
    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;
    @RequestMapping(path="/letter/list", method = RequestMethod.GET)
    public String getLetterList(Model model, Page page){
//        Integer.valueOf("abc");
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/letter/list");
        page.setRows(messageService.findConversationCount(user.getId()));
        List<Message> conversationList = messageService.findConversations(user.getId(), page.getOffset(), page.getLimit());
        List<Map<String, Object>> conversations = new ArrayList<>();
        System.out.println(user.getId());
        if(conversationList != null){
            for(Message message: conversationList){
                Map<String, Object> map = new HashMap<>();
                System.out.println(message);
                map.put("conversation", message);
                map.put("letterCount", messageService.findLetterCount(message.getConversationId()));
                map.put("unreadCount", messageService.findUnreadLetterCount(user.getId(), message.getConversationId()));
                int targetId  = (user.getId() == message.getFromId()? message.getToId() : message.getFromId());
                map.put("targetId", userService.findUserById(targetId));
                conversations.add(map);
                System.out.println("unreadCount"+map.get("unreadCount").toString());
                System.out.println("letterCount"+map.get("letterCount").toString());
            }
        }
        model.addAttribute("conversations", conversations);
        int totalUnreadLetterCount = messageService.findUnreadLetterCount(user.getId(), null);
        model.addAttribute("totalUnreadLetterCount", totalUnreadLetterCount);
        int totalUnreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), null);
        model.addAttribute("totalUnreadNoticeCount", totalUnreadNoticeCount);
        return "/site/letter";
    }

    @RequestMapping(path = "/letter/detail/{conversationId}", method = RequestMethod.GET)
    public String getLetterDetail(@PathVariable("conversationId") String conversationId, Page page, Model model){
        page.setLimit(5);
        page.setPath("/letter/detail/"+conversationId);
        page.setRows(messageService.findLetterCount(conversationId));
        List<Message> letterList = messageService.findLetters(conversationId, page.getOffset(), page.getLimit());
        List<Map<String, Object>> letters = new ArrayList<>();
        if(letterList !=null){
            for(Message message : letterList){
                Map<String, Object> map = new HashMap<>();
                map.put("letter", message);
                map.put("fromUser", userService.findUserById(message.getFromId()));
                letters.add(map);
            }
        }
        model.addAttribute("letters", letters);
        model.addAttribute("target", getLetterTarget(conversationId));
        List<Integer> ids = getLetterIds(letterList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "/site/letter-detail";
    }

    private User getLetterTarget(String conversationId){
        String[] ids = conversationId.split("_");
        int id0 = Integer.parseInt(ids[0]);
        int id1 = Integer.parseInt(ids[1]);
        if(hostHolder.getUser().getId() == id0){
            return userService.findUserById(id1);
        }else {
            return userService.findUserById(id0);
        }
    }

    private List<Integer> getLetterIds(List<Message> letterList){
        List<Integer> ids = new ArrayList<>();
        if (letterList != null){
            for (Message message : letterList){
                if(hostHolder.getUser().getId() == message.getToId() && message.getStatus() == 0){
                    ids.add(message.getId());
                }
            }
        }
        return ids;
    }

    @RequestMapping(path = "/letter/send", method=RequestMethod.POST)
    @ResponseBody //因为是异步的，所以要加上这个注解
    public String sendLetter(String toName, String content){
//        Integer.valueOf("abc");
        User target = userService.findUserByName(toName);
        System.out.println("toName = " + toName + ", content = " + content);
        if(target == null){
            return ForumUtil.getJSONString(1, "目标用户不存在！");
        }
        Message message = new Message();
        message.setFromId(hostHolder.getUser().getId());
        message.setToId(target.getId());
        if(message.getFromId() < message.getToId()){
            message.setConversationId(message.getFromId() +"_"+message.getToId());
        }else{
            message.setConversationId(message.getToId() +"_"+message.getFromId());
        }
        message.setContent(content);
        message.setCreateTime(new Date());
        messageService.addMessage(message);
        return ForumUtil.getJSONString(0);
    }

    @RequestMapping(path = "/notice/list", method = RequestMethod.GET)
    public String getNoticeList(Model model){
        User user = hostHolder.getUser();
        Message message = messageService.findLatestNotice(user.getId(), TOPIC_COMMENT);
        Map<String, Object> messageVo = new HashMap<>();
        messageVo.put("message", message);
        if (message!=null){
            String content  = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));
            int noticeCount = messageService.findNoticeCount(user.getId(),TOPIC_COMMENT);
            messageVo.put("noticeCount", noticeCount);
            int unreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), TOPIC_COMMENT);
            messageVo.put("unreadNoticeCount", unreadNoticeCount);
        }
        model.addAttribute("comment", messageVo);

        // like
        message = messageService.findLatestNotice(user.getId(), TOPIC_LIKE);
        messageVo = new HashMap<>();
        messageVo.put("message", message);
        if (message!=null){
            String content  = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            messageVo.put("postId", data.get("postId"));
            int noticeCount = messageService.findNoticeCount(user.getId(),TOPIC_LIKE);
            messageVo.put("noticeCount", noticeCount);
            int unreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), TOPIC_LIKE);
            messageVo.put("unreadNoticeCount", unreadNoticeCount);
        }
        model.addAttribute("like", messageVo);

        message = messageService.findLatestNotice(user.getId(), TOPIC_FOLLOW);
        messageVo = new HashMap<>();
        messageVo.put("message", message);
        if (message!=null){
            String content  = HtmlUtils.htmlUnescape(message.getContent());
            Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
            messageVo.put("user", userService.findUserById((Integer) data.get("userId")));
            messageVo.put("entityType", data.get("entityType"));
            messageVo.put("entityId", data.get("entityId"));
            int noticeCount = messageService.findNoticeCount(user.getId(),TOPIC_FOLLOW);
            messageVo.put("noticeCount", noticeCount);
            int unreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), TOPIC_FOLLOW);
            messageVo.put("unreadNoticeCount", unreadNoticeCount);
        }
        model.addAttribute("follow", messageVo);

        // total unread
        int totalUnreadLetterCount = messageService.findUnreadLetterCount(user.getId(), null);
        int totalUnreadNoticeCount = messageService.findUnreadNoticeCount(user.getId(), null);
        model.addAttribute("totalUnreadLetterCount", totalUnreadLetterCount);
        model.addAttribute("totalUnreadNoticeCount", totalUnreadNoticeCount);
        return "/site/notice";
    }

    @RequestMapping(path = "/notice/detail/{topic}", method=RequestMethod.GET)
    public String getNoticeDetail(@PathVariable("topic") String topic, Page page, Model model){
        User user = hostHolder.getUser();
        page.setLimit(5);
        page.setPath("/notice/detail/"+topic);
        page.setRows(messageService.findNoticeCount(user.getId(), topic));
        List<Message> noticeList = messageService.findNotices(user.getId(), topic, page.getOffset(), page.getLimit());
        List<Map<String, Object>> noticeVoList = new ArrayList<>();
        if (noticeList!=null){
            for(Message notice : noticeList){
                Map<String, Object> map = new HashMap<>();
                map.put("notice", notice);
                String content = HtmlUtils.htmlUnescape(notice.getContent());
                Map<String, Object> data = JSONObject.parseObject(content, HashMap.class);
                map.put("user", userService.findUserById((Integer) data.get("userId")) );
                map.put("entityType", data.get("entityType"));
                map.put("entityId", data.get("entityId"));
                map.put("postId", data.get("postId"));
                map.put("fromUser", userService.findUserById(notice.getFromId()));
                noticeVoList.add(map);
            }
        }
        model.addAttribute("notices", noticeVoList);

        // set the status from unread to read

        List<Integer> ids = getLetterIds(noticeList);
        if(!ids.isEmpty()){
            messageService.readMessage(ids);
        }
        return "/site/notice-detail";
    }
}


