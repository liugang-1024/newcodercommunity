package com.newcoder.community;

import com.newcoder.community.dao.DiscussPostMapper;
import com.newcoder.community.dao.LoginTicketMapper;
import com.newcoder.community.dao.MessageMapper;
import com.newcoder.community.dao.UserMapper;
import com.newcoder.community.entity.LoginTicket;
import com.newcoder.community.entity.Message;
import com.newcoder.community.entity.User;
import com.newcoder.community.entity.DiscussPost;
import com.newcoder.community.util.CommunityUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Date;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class MapperTests {

    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DiscussPostMapper discussPostMapper;
    @Autowired
    private LoginTicketMapper loginTicketMapper;
    @Autowired
    private MessageMapper messageMapper;

    @Test
    public void testSelectUser(){
        User user = userMapper.selectById(101);
        System.out.println(user.toString());
    }

    @Test
    public void testSelectPosts(){
        List<DiscussPost> discussPosts = discussPostMapper.selectDiscussPosts(0, 0, 10);
        for (DiscussPost post:discussPosts) {
            System.out.println(post);
        }
    }

    @Test
    public void testInsertLoginTicket(){
        LoginTicket loginTicket = new LoginTicket();
        loginTicket.setUserId(101);
        loginTicket.setTicket("asdhasjd");
        loginTicket.setStatus(0);
        loginTicket.setExpired(new Date(System.currentTimeMillis() + 1000*60*10));
        loginTicketMapper.insertLoginTicket(loginTicket);
    }

    @Test
    public void ttt(){
        String s = CommunityUtil.MD5("liuchun8806.167f9");
        System.out.println(s);
    }

    @Test
    public void testSelectLetters(){
        List<Message> messages = messageMapper.selectConversations(111, 0, 20);
        for (Message m:messages) {
            System.out.println(m);
        }
        int count = messageMapper.selectConversionCount(111);
        System.out.println(count);

        List<Message> messages1 = messageMapper.selectLetters("111_112", 0, 10);
        for (Message m:messages1) {
            System.out.println(m);
        }
        count = messageMapper.selectLetterCount("111_112");
        System.out.println(count);

        count = messageMapper.selectLetterUnreadCount(131, "111_131");
        System.out.println(count);
    }


}
