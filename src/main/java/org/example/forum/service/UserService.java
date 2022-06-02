package org.example.forum.service;

import org.apache.commons.lang3.StringUtils;
import org.example.forum.dao.LoginTicketMapper;
import org.example.forum.dao.UserMapper;
import org.example.forum.entity.LoginTicket;
import org.example.forum.entity.User;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.example.forum.util.MailClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class UserService implements ForumConstant {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Autowired
    private LoginTicketMapper loginTicketMapper;

    @Value("${forum.path.domain}")
    private String domain;
    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }

    public Map<String, Object> register(User user){
        Map<String, Object> map= new HashMap<>();

        if (user==null){
            throw new IllegalArgumentException("Invalid parameter of User");
        }
        if (StringUtils.isBlank(user.getUsername())){
            map.put("usernameMsg", "username can not be blank");
            return map;
        }
        if (StringUtils.isBlank(user.getPassword())){
            map.put("passwordMsg", "password can not be blank");
            return map;
        }
        if (StringUtils.isBlank(user.getEmail())){
            map.put("emailMsg", "email can not be blank");
            return map;
        }
        // verify existence
        User u = userMapper.selectByName(user.getUsername());
        if(u != null){
            map.put("usernameMsg", "username is taken");
            return map;
        }
        u = userMapper.selectByEmail(user.getEmail());
        if(u != null){
            map.put("emailMsg", "email is registered");
            return map;
        }

        // register
        user.setSalt(ForumUtil.generateUUID().substring(0,5));
        user.setPassword(ForumUtil.md5(user.getPassword()+user.getSalt()));
        user.setType(0); //普通用户
        user.setStatus(0); //没有激活
        user.setActivationCode(ForumUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png", new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        // activation

        Context context = new Context();
        context.setVariable("email", user.getEmail());
        String url = domain+contextPath+"/activation/"+user.getId()+"/"+user.getActivationCode(); // mysql will auto-generate an increasing id for a new user
        context.setVariable("url", url);
        String content = templateEngine.process("/mail/activation", context);
        mailClient.sendMail(user.getEmail(), "Account Activation needed", content);
        return map;
    }

    public int activate(int userId, String activationCode) {
        User user = userMapper.selectById(userId);
        if(user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        }
        if(user.getActivationCode().equals(activationCode)) {
            userMapper.updateStatus(userId, 1);
            return ACTIVATION_SUCCESS;
        }
        return ACTIVATION_FAILURE;
    }

    public Map<String,Object> login(String username, String password, int expiredSeconds) {
        Map<String, Object> map = new HashMap<> ();
        if (StringUtils.isBlank(username)) {
            map.put("usernameMsg", "Blank username not allowed!");
            return map;
        }
        if (StringUtils.isBlank(username)) {
            map.put("passwordMsg", "Blank password not allowed!");
            return map;
        }

        User user = userMapper.selectByName(username);
        if (user == null) {
            map.put("usernameMsg", "username does not exists");
            return map;
        }
        if (user.getStatus() == 0) {
            map.put("usernameMsg", "account not activated!");
            return map;
        }
        password = ForumUtil.md5(password + user.getSalt());
        if (!user.getPassword().equals(password)) {
            map.put("passwordMsg", "Incorrect Password!");
            return map;
        }
        // generate the login ticket
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(ForumUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000L)); //这个地方写成1000L，否则就可能溢出，导致设置失败
        System.out.println(loginTicket.getExpired());
        loginTicketMapper.insertLoginTicket(loginTicket); // 这一部分相当于session. 只不过我们没有用session存，而是把ticket存到数据库，将来通过请求里的ticket来找到用户，就能知道是谁
        map.put("ticket", loginTicket.getTicket());
        return map;
    }

    public void logout(String ticket){
        loginTicketMapper.updateStatus(ticket, 1);
    }

    public LoginTicket findLoginTicket(String ticket){
        return loginTicketMapper.selectByTicket(ticket);
    }

    public int updateHeader(int userId, String headerUrl){
        return userMapper.updateHeader(userId, headerUrl);
    }
    public int updatePassword(int userId, String password) {return userMapper.updatePassword(userId, password);}
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }
}
