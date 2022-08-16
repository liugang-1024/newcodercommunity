package com.newcoder.community.service;

import com.newcoder.community.dao.LoginTicketMapper;
import com.newcoder.community.dao.UserMapper;
import com.newcoder.community.entity.LoginTicket;
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
    private LoginTicketMapper loginTicketMapper;

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
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if (StringUtils.isAllBlank(user.getPassword())){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if (user.getPassword().length()<8){
            map.put("passwordMsg","密码长度不能低于8位");
            return map;
        }
        if (StringUtils.isAllBlank(user.getEmail())){
            map.put("emailMsg","邮箱不能为空");
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

    //登录
    public Map<String,Object> login(String username,String password,long expiredSeconds){
        Map<String,Object> map = new HashMap<>();

        //空值处理
        if (StringUtils.isBlank(username)){
            map.put("usernameMsg","账号不能为空");
            return map;
        }
        if (StringUtils.isBlank(password)){
            map.put("passwordMsg","密码不能为空");
            return map;
        }
        if (password.length()<8){
            map.put("passwordMsg","密码长度不能低于8位");
            return map;
        }
        //验证账号
        User user = userMapper.selectByName(username);
        if (user == null){
            map.put("usernameMsg","该账号不存在");
            return map;
        }
        //验证密码
        if (user.getStatus()==0){
            map.put("usernameMsg","该账号未激活");
            return map;
        }
        //验证密码
        password = CommunityUtil.MD5(password+user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("passwordMsg","密码不正确");
            return map;
        }
        //生成登录凭证
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(user.getId());
        loginTicket.setTicket(CommunityUtil.generateUUID());
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + expiredSeconds * 1000));
        map.put("ticket",loginTicket.getTicket());
        loginTicketMapper.insertLoginTicket(loginTicket);
        return map;
    }

    public void  logout(String ticket){
        loginTicketMapper.updateStatusByTicket(ticket,1);
    }

    public LoginTicket findLoginTicket(String ticket){
        return loginTicketMapper.selectByTicket(ticket);
    }

    public int updateHeader(int userId,String headerUrl){
        return userMapper.updateHeader(userId,headerUrl);
    }

    //验证修改密码时密码是否合法，原密码是否正确
    public Map<String,Object> updatePasswd(String oldPassword,String newPassword,User user){
        Map<String,Object> map = new HashMap<>();
        if (StringUtils.isBlank(oldPassword) || StringUtils.isBlank(newPassword)){
            map.put("updateerrorMsg","密码不能为空");
            return map;
        }
        String password = CommunityUtil.MD5(oldPassword+user.getSalt());
        if (!user.getPassword().equals(password)){
            map.put("updateerrorMsg","原密码错误");
            return map;
        }
        newPassword = CommunityUtil.MD5(newPassword+user.getSalt());
        userMapper.updatePassword(user.getId(), newPassword);
        map.put("updatesuccessdMsg","修改成功");
        return map;
    }
    public User findUserByName(String username){
        return userMapper.selectByName(username);
    }

}
