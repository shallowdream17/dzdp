package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.PageUtil;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Autowired
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 发送验证码
     * @param phone
     * @param session
     * @return
     */
    @Override
    public Result sendCode(String phone, HttpSession session) {
        if(RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误!");
        }
        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        //保存验证码到session
        //session.setAttribute("code",code);
        //session.setAttribute("phone",phone );
        //保存验证码到redis
        stringRedisTemplate.opsForValue().set(RedisConstants.LOGIN_CODE_KEY+phone,code,RedisConstants.LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //模拟发送验证码
        log.info("验证码为{},请勿告诉他人",code);
        return Result.ok();
    }

    /**
     * 用户登陆
     * @param loginForm
     * @param session
     * @return
     */
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号和验证码
//        Object phone = session.getAttribute("phone");
//        Object code = session.getAttribute("code");
//        if(phone==null||!phone.toString().equals(loginForm.getPhone())
//            ||code==null||!code.toString().equals(loginForm.getCode())){
//            return Result.fail("手机号或验证码错误");
//        }
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + loginForm.getPhone());
        log.info("code:{}",code);

        if(code==null||code.isEmpty()||!code.equals(loginForm.getCode())){
            return Result.fail("手机号或验证码错误");
        }
        //查询该手机号是否已经注册
        User user = userMapper.queryByPhone(loginForm.getPhone());
        //用户不存在则创建新用户
        if(user==null){
            user = getNewUser(loginForm.getPhone());
            userMapper.addUser(user);
        }
        //将用户保存到redis中
        //生成随机token作为登陆令牌，ps：可以改为jwt感觉
        String token = UUID.randomUUID().toString();
        //将user对象转化为hash存储
        UserDTO userDTO = new UserDTO();
        BeanUtils.copyProperties(user,userDTO);
        //Map<String, Object> stringObjectMap = BeanUtil.beanToMap(userDTO);
        Map<String, Object> stringObjectMap = new HashMap<>();
        stringObjectMap.put("id",userDTO.getId().toString());
        stringObjectMap.put("icon",userDTO.getIcon());
        stringObjectMap.put("nickName",userDTO.getNickName());

        stringRedisTemplate.opsForHash().putAll(RedisConstants.LOGIN_USER_KEY+token,stringObjectMap);
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY+token,RedisConstants.LOGIN_USER_TTL,TimeUnit.SECONDS);
        //session.setAttribute("user",user);
        //log.info("session已保存user{}",user);
        //返回token
        return Result.ok(token);
    }

    /**
     * 创建新用户
     * @param phone
     * @return
     */
    private User getNewUser(String phone) {
        User user = new User();
        user.setCreateTime(LocalDateTime.now());
        user.setIcon("https://shallowdreams17.oss-cn-hangzhou.aliyuncs.com/984bf524-7929-4b39-8b27-5a0b23160b9e.jpg");
        user.setUpdateTime(LocalDateTime.now());
        user.setPassword("123456");
        user.setNickName("aniu_"+RandomUtil.randomString(10));
        user.setPhone(phone);
        return user;
    }

}
