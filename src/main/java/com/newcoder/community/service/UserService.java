package com.newcoder.community.service;

import com.newcoder.community.dao.UserMapper;
import com.newcoder.community.entity.User;
import com.newcoder.community.util.CommunityConstant;
import com.newcoder.community.util.CommunityUtil;
import com.newcoder.community.util.MailClient;
import org.apache.commons.lang3.StringUtils;
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
public class UserService implements CommunityConstant {
    @Autowired
    private UserMapper userMapper;

    @Autowired
    private MailClient mailClient;

    @Autowired
    private TemplateEngine templateEngine;

    @Value("${community.path.domain}")
    private String domain;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    public User findUserById(int id){
        return userMapper.selectById(id);
    }


    //注册业务
    public Map<String ,Object> registry(User user){
        Map<String ,Object> map = new HashMap<>();

        //空值判断
        if (user==null){
            throw new IllegalArgumentException("参数不能为空");
        }
        if (StringUtils.isAllBlank(user.getUsername())){
            map.put("username","账号不能为空");
            return map;
        }
        if (StringUtils.isAllBlank(user.getPassword())){
            map.put("password","密码不能为空");
            return map;
        }
        if (StringUtils.isAllBlank(user.getEmail())){
            map.put("email","邮箱不能为空");
            return map;
        }

        //验证账号
        User u = userMapper.selectByName(user.getUsername());
        if (u!=null){
            map.put("usernameMsg","该账号已存在");
            return map;
        }

        //验证邮箱
        u = userMapper.selectByEmail(user.getEmail());
        if (u!=null){
            map.put("emailMsg","该邮箱已存在");
            return map;
        }

        //注册用户
        user.setSalt(CommunityUtil.generateUUID().substring(0,5));
        user.setPassword(CommunityUtil.MD5(user.getPassword()+user.getSalt()));
        user.setStatus(0);
        user.setActivationCode(CommunityUtil.generateUUID());
        user.setHeaderUrl(String.format("http://images.nowcoder.com/head/%dt.png",new Random().nextInt(1000)));
        user.setCreateTime(new Date());
        userMapper.insertUser(user);

        //给用户发送激活码邮件
        Context context = new Context();
        context.setVariable("email",user.getEmail());
        //http://localhost:8080/community/activation/101/code
        String url = domain + contextPath + "/activation/" + user.getId() + "/" + user.getActivationCode();
        context.setVariable("url",url);
        String content = templateEngine.process("/mail/activation",context);
        mailClient.sendMail(user.getEmail(),"激活账号",content);
        return map;
    }

    //激活账户
    public int activation(int userId,String code){
        User user = userMapper.selectById(userId);
        if (user.getStatus() == 1){
            return ACTIVATION_REPEAT;
        } else if (user.getActivationCode().equals(code)) {
            userMapper.updateStatus(userId,1);
            return ACTIVATION_SUCCESS;
        }else {
            return ACTIVATION_FAIL;
        }
    }




}
