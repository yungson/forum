package org.example.forum.controller;

import com.google.code.kaptcha.Producer;
import org.apache.commons.lang3.StringUtils;
import org.example.forum.entity.User;
import org.example.forum.service.UserService;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.example.forum.util.RedisKeyUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.imageio.ImageIO;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller
public class LoginController implements ForumConstant {

    public static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    @Autowired
    private UserService userService;

    @Autowired
    private Producer kaptchaProducer;

    @Value("${server.servlet.context-path}")
    private String contextPath;
    @Autowired
    private RedisTemplate redisTemplate;

    @RequestMapping(path="/register", method = RequestMethod.GET)
    public String getResisterPage() {
        return "/site/register";
    }

    @RequestMapping(path="/login", method = RequestMethod.GET)
    public String getLoginPage() {
        return "/site/login";
    }

    @RequestMapping(path = "/register", method = RequestMethod.POST)
    public String register(Model model, User user){
        Map<String, Object> map = userService.register(user);
        if (map == null || map.isEmpty()){
            model.addAttribute("msg", "Success! Please check your email to activate your account");
            model.addAttribute("target", "/index");
            return "/site/operate-result";
        }else {
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            model.addAttribute("emailMsg", map.get("emailMsg"));
            return "/site/register";
        }
    }

    @RequestMapping(path="/activation/{userId}/{code}", method = RequestMethod.GET)
    public String activation(Model model, @PathVariable("userId") int userId, @PathVariable("code") String code) {
        int result = userService.activate(userId, code);
        if(result==ACTIVATION_SUCCESS) {
            model.addAttribute("msg", "Your account is successfully activated!");
            model.addAttribute("target","/login");
        } else if(result==ACTIVATION_REPEAT){
            model.addAttribute("msg", "Your account has been activated before!");
            model.addAttribute("target","/index");
        } else {
            model.addAttribute("msg", "Failure! Incorrect activation code");
            model.addAttribute("target", "/index");
        }
        return "/site/operate-result";
    }

//    @RequestMapping(path="/kaptcha", method = RequestMethod.GET)
//    public void getKaptcha(HttpServletResponse response, HttpSession session){
//        //验证码生成以后，需要存在服务端（不能存在浏览器端，要不然挺容易被比如爬虫之类的盗取），生成之后我们要记住这个信息然后之后登陆的时候
//        // 判断是不是一致，所以这就是跨请求了。可以采用sessionID的方式进行实现。为什么不用cookie?因为验证码属于敏感信息，不能用cookie存放在浏览器端。
//        // 而session这种方式，浏览器端只有一个sessionId不影响。参见AlphaController中的示例。
//        String text = kaptchaProducer.createText();
//        BufferedImage image = kaptchaProducer.createImage(text);
//        // 验证码存入session
//        session.setAttribute("kaptcha", text);
//        // 设置传输文件格式
//        response.setContentType("image/png");
//        try {
//            OutputStream os = response.getOutputStream();
//            ImageIO.write(image, "png", os);
//        } catch (IOException e) {
//            logger.error("Get kaptcha failuer!");
//        }
//    }

    @RequestMapping(path="/kaptcha", method = RequestMethod.GET)
    public void getKaptcha(HttpServletResponse response, HttpSession session){
        //验证码生成以后，需要存在服务端（不能存在浏览器端，要不然挺容易被比如爬虫之类的盗取），生成之后我们要记住这个信息然后之后登陆的时候
        // 判断是不是一致，所以这就是跨请求了。可以采用sessionID的方式进行实现。为什么不用cookie?因为验证码属于敏感信息，不能用cookie存放在浏览器端。
        // 而session这种方式，浏览器端只有一个sessionId不影响。参见AlphaController中的示例。
        String text = kaptchaProducer.createText();
        BufferedImage image = kaptchaProducer.createImage(text);
//        // 验证码存入session
//        session.setAttribute("kaptcha", text);
        // 生成尝试登录用户遇到的验证码标识kaptchOwner
        String kaptchaOwner = ForumUtil.generateUUID();
        Cookie cookie = new Cookie("kaptchaOwner", kaptchaOwner);
        cookie.setMaxAge(60);
        cookie.setPath(contextPath);
        response.addCookie(cookie);
        // 验证码存入redis
        String redisKey = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
        redisTemplate.opsForValue().set(redisKey, text, 60, TimeUnit.SECONDS);

        // 设置传输文件格式
        response.setContentType("image/png");
        try {
            OutputStream os = response.getOutputStream();
            ImageIO.write(image, "png", os);
        } catch (IOException e) {
            logger.error("Get kaptcha failuer!");
        }
    }

//    @RequestMapping(path="/login", method = RequestMethod.POST)
//    public String login(Model model, String username, String password, String code,  boolean rememberMe, HttpSession session, HttpServletResponse response){
//        // code是验证码, 是用户提交的验证码, 而之前我们通过生成验证码的时候是将验证码的答案根据sessionId存放在session里面的，所以此处也需要session这个参数
//        // 如果登陆成功，我们会生成login_ticket, 发给用户让用户保存，保存是通过session实现的，所以我们还需要HttpSerlvetResponse 这个参数 将其放到session里
//        // 如果login里面的参数不是普通参数，比如是User这样的参数，那么spring就会自动把User这样的参数注入到model对象里，那么这种情况下在login.html模版里就可以访问user
//        // 但是如果是普通参数，比如int, string 就不会执行此操作。那如何在login.html模版里实现如果用户输入用户名或密码错误继续保留原来的值在表单里呢？也就是如何在login.html模版里
//        // 获取username, password这些值？这些值是存在request里的，如果输入错误，request依然是存在的，我们仍然可以通过request来获取这些值，获取的方法是"${param.password}"
//        // 参见login.html
//        String kaptcha = (String) session.getAttribute("kaptcha"); // (String)强制类型转换是因为getAttrbribute返回的是object类型（即使你当初存的时候也是存String）
//        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
//            model.addAttribute("codeMsg", "Incorrect kaptcha!");
//            return "/site/login";
//        }
//        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
//        System.out.println(expiredSeconds);
//        Map<String, Object> map = userService.login(username, password, expiredSeconds);
//        if(map.containsKey("ticket")){
//            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
//            cookie.setPath(contextPath);
//            cookie.setMaxAge(expiredSeconds);
//            response.addCookie(cookie);
//            return "redirect:/index";
//        }else{
//            model.addAttribute("usernameMsg", map.get("usernameMsg"));
//            model.addAttribute("passwordMsg", map.get("passwordMsg"));
//            return "/site/login";
//        }
//    }

    @RequestMapping(path="/login", method = RequestMethod.POST)
    public String login(Model model, String username, String password, String code,  boolean rememberMe,
                        HttpServletResponse response, @CookieValue("kaptchaOwner") String kaptchaOwner){
        // code是验证码, 是用户提交的验证码, 而之前我们通过生成验证码的时候是将验证码的答案根据sessionId存放在session里面的，所以此处也需要session这个参数
        // 如果登陆成功，我们会生成login_ticket, 发给用户让用户保存，保存是通过session实现的，所以我们还需要HttpSerlvetResponse 这个参数 将其放到session里
        // 如果login里面的参数不是普通参数，比如是User这样的参数，那么spring就会自动把User这样的参数注入到model对象里，那么这种情况下在login.html模版里就可以访问user
        // 但是如果是普通参数，比如int, string 就不会执行此操作。那如何在login.html模版里实现如果用户输入用户名或密码错误继续保留原来的值在表单里呢？也就是如何在login.html模版里
        // 获取username, password这些值？这些值是存在request里的，如果输入错误，request依然是存在的，我们仍然可以通过request来获取这些值，获取的方法是"${param.password}"
        // 参见login.html
        String kaptcha = null;
        if (StringUtils.isNotBlank(kaptchaOwner)){
            String redisKey  = RedisKeyUtil.getKaptchaKey(kaptchaOwner);
            kaptcha = (String) redisTemplate.opsForValue().get(redisKey);
        }
        if(StringUtils.isBlank(kaptcha) || StringUtils.isBlank(code) || !kaptcha.equalsIgnoreCase(code)){
            model.addAttribute("codeMsg", "Incorrect kaptcha!");
            return "/site/login";
        }
        int expiredSeconds = rememberMe ? REMEMBER_EXPIRED_SECONDS:DEFAULT_EXPIRED_SECONDS;
        System.out.println(expiredSeconds);
        Map<String, Object> map = userService.login(username, password, expiredSeconds);
        if(map.containsKey("ticket")){
            Cookie cookie = new Cookie("ticket", map.get("ticket").toString());
            cookie.setPath(contextPath);
            cookie.setMaxAge(expiredSeconds);
            response.addCookie(cookie);
            return "redirect:/index";
        }else{
            model.addAttribute("usernameMsg", map.get("usernameMsg"));
            model.addAttribute("passwordMsg", map.get("passwordMsg"));
            return "/site/login";
        }
    }

    @RequestMapping(path = "/logout", method = RequestMethod.GET)
    public String logout(@CookieValue("ticket") String ticket){
        userService.logout(ticket);
        return "redirect:/login"; //login有两个，重定向一般都是默认定向到get请求
    }
}
