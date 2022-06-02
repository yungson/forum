package org.example.forum.controller;

import org.apache.commons.lang3.StringUtils;
import org.example.forum.annotation.LoginRequired;
import org.example.forum.entity.User;
import org.example.forum.service.FollowService;
import org.example.forum.service.LikeService;
import org.example.forum.service.UserService;
import org.example.forum.util.ForumConstant;
import org.example.forum.util.ForumUtil;
import org.example.forum.util.HostHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

@Controller
@RequestMapping(path = "/user")
public class UserController implements ForumConstant {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${forum.path.upload}")
    private String uploadPath;

    @Value("${forum.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @Autowired
    private LikeService likeService;

    @Autowired
    private FollowService followService;

    @RequestMapping(path = "/setting", method = RequestMethod.GET)
    @LoginRequired
    public String getSettingPage(){
        return "/site/setting";
    }

    @RequestMapping(path = "/upload", method = RequestMethod.POST)
    @LoginRequired
    public String uploadHeader(MultipartFile headerImage, Model model) {
        if (headerImage == null){
            model.addAttribute("error", "Please select your header Image!");
            return "/site/setting";
        }
        String fileName = headerImage.getOriginalFilename();
        if(fileName.lastIndexOf(".") == -1){
            model.addAttribute("error", "Incorrect file format!");
            return "/site/setting";
        }
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        System.out.println(suffix);
        if (!suffix.equals(".png") && !suffix.equals(".jpg") && !suffix.equals(".jpeg")) {
            model.addAttribute("error", "Incorrect file format!");
            return "/site/setting";
        }

        fileName = ForumUtil.generateUUID()+suffix;
        File dest = new File(uploadPath+"/"+fileName);
        try{
            headerImage.transferTo(dest);
        }catch(IOException e){
            logger.error("上传文件失败："+e.getMessage());
            throw new RuntimeException("上传文件失败， 服务器发生异常", e);
        }
        User user = hostHolder.getUser();
        String headerUrl = domain + contextPath + "/user/header/"+fileName;
        userService.updateHeader(user.getId(), headerUrl);
        return "redirect:/index";
    }

    @RequestMapping(path = "/chpassword",method = RequestMethod.POST)
    @LoginRequired
    public String chPassword(Model model, String oldPassword, String newPassword){
        if (StringUtils.isBlank(oldPassword) ){
            model.addAttribute("oldPasswordMsg", "Old password can not be null");
            return "/site/setting";
        }
        if (StringUtils.isBlank(oldPassword) ){
            model.addAttribute("newPasswordMsg", "Old password can not be null");
            return "/site/setting";
        }
        User user = hostHolder.getUser();
        oldPassword = ForumUtil.md5(oldPassword+user.getSalt());
        if(!oldPassword.equals(user.getPassword())) {
            model.addAttribute("oldPasswordMsg", "Incorrect Old password");
            return "/site/setting";
        }
        newPassword = ForumUtil.md5(newPassword+user.getSalt());
        userService.updatePassword(user.getId(), newPassword);
        model.addAttribute("msg", "密码修改成功");
        model.addAttribute("target","/index");
        return "redirect:/index";
        // return "redirect:/site/operate-result"; //难道是model不会传入重定向的链接中去？
    }

    @RequestMapping(path="/header/{fileName}", method = RequestMethod.GET)
    public void getHeader(@PathVariable("fileName") String fileName, HttpServletResponse response) { //当向浏览器响应二进制数据的时候，一般返回值设置成void,然后通过输出流传输
        fileName = uploadPath+"/"+fileName;
        String suffix = fileName.substring(fileName.lastIndexOf("."));
        response.setContentType("image/"+suffix);
        try (
                FileInputStream fis = new FileInputStream(fileName); //输入流不会自动关闭，所以需要放到try()里面管理
        ){
            OutputStream os = response.getOutputStream(); //输出流会自动关闭
            byte[] buffer = new byte[1024];
            int b = 0;
            while((b = fis.read(buffer))!= -1){
                os.write(buffer, 0, b);
            }
        } catch(IOException e){
            logger.error("获取头像失败:"+e.getMessage());
        }
    }

    @RequestMapping(path = "/profile/{userId}", method = RequestMethod.GET)
    public String getProfilePage(@PathVariable("userId") int userId, Model model){
        User user = userService.findUserById(userId);
        if (user==null){
            throw new RuntimeException("User does not exist");
        }
        model.addAttribute("user", user);
        int likeCount = likeService.findUserLikeCount(userId);
        model.addAttribute("likeCount", likeCount);
        // 关注数量
        long followeeCount = followService.findFolloweeCount(userId, ENTITY_TYPE_USER);
        model.addAttribute("followeeCount", followeeCount);
        // fan numbers
        long followerCount = followService.findFollowerCount(ENTITY_TYPE_USER, userId);
        model.addAttribute("followerCount", followerCount);
        System.out.println(followerCount);
        // whether followed or not
        boolean hasFollowed = false;
        if (hostHolder.getUser() !=null){
            hasFollowed = followService.findHasFollowed(hostHolder.getUser().getId(), ENTITY_TYPE_USER, userId);
        }
        System.out.println(hasFollowed);
        model.addAttribute("hasFollowed", hasFollowed);

        return "/site/profile";
    }
}
