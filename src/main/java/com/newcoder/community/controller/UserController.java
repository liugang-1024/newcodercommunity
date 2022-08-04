package com.newcoder.community.controller;

import com.newcoder.community.annotation.LoginRequired;
import com.newcoder.community.entity.User;
import com.newcoder.community.service.UserService;
import com.newcoder.community.util.CommunityUtil;
import com.newcoder.community.util.HostHolder;
import org.apache.commons.lang3.StringUtils;
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
import java.util.Map;


@Controller
@RequestMapping("/user")
public class UserController {
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    @Value("${community.path.upload}")
    private String uploadPath;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextpath;

    @Autowired
    private UserService userService;

    @Autowired
    private HostHolder hostHolder;

    @LoginRequired
    @RequestMapping(path = "/setting",method = RequestMethod.GET)
    public String getSettingPage(){
        return "/site/setting";
    }

    //上传头像
    @LoginRequired
    @RequestMapping(path = "/upload",method = RequestMethod.POST)
    public String uploadHeader(MultipartFile headerImage, Model model){
        if (headerImage==null){
            model.addAttribute("error","您还没有选择图片!");
            return "/site/setting";
        }
        //截取文件后缀，并判断其是否为空
        String filename = headerImage.getOriginalFilename();
        String suffix = filename.substring(filename.lastIndexOf("."));
        if (StringUtils.isBlank(suffix)){
            model.addAttribute("error","文件格式不正确");
            return "/site/setting";
        }

        //生成随机文件名
        filename = CommunityUtil.generateUUID()+suffix;
        File dest = new File(uploadPath+ "/" + filename);
        try {
            //存储文件
            headerImage.transferTo(dest);
        } catch (IOException e) {
            logger.error("上传文件失败"+e.getMessage());
            throw new RuntimeException("上传文件失败，服务器发生异常！",e);
        }
        //更新当前用户头像路径(web访问路径)
        //http://localhost:8080/community/user/header/xxx.png
        User user = hostHolder.getUser();
        String headerUrl = domain  + contextpath + "/user/header/" + filename;
        userService.updateHeader(user.getId(),headerUrl);
        return "redirect:/index";
    }

    //获取头像文件
    @RequestMapping(path = "/header/{filename}",method = RequestMethod.GET)
    public void getHeader(@PathVariable("filename") String filename, HttpServletResponse response){
        //服务器存放的路径就是uploadPath + "/" + filename
        filename = uploadPath + "/" + filename;
        //文件后缀解析
        String suffix = filename.substring(filename.lastIndexOf("."));
        //响应图片
        response.setContentType("image/" + suffix);
        try (
                FileInputStream fis = new FileInputStream(filename);
                OutputStream os = response.getOutputStream();
                ){
            byte[] buffer = new byte[1024];
            int b=0;
            while ((b = fis.read(buffer)) != -1){
                os.write(buffer,0,b);
            }
        } catch (IOException e) {
            logger.error("读取头像失败",e.getMessage());
        }
    }

    //修改密码
    @RequestMapping(path = "/update",method = RequestMethod.POST)
    public String updatePassword(String oldPassword,String newPassword,Model model){
       //检查密码
        User user = hostHolder.getUser();
        Map<String,Object> updatepasswd = userService.updatePasswd(oldPassword,newPassword,user);
        if (updatepasswd.containsKey("updateerrorMsg")){
            model.addAttribute("updateerrorMsg",updatepasswd.get("updateerrorMsg"));
            return "/site/setting";
        }else {
            return "redirect:/index";
        }

    }

}
