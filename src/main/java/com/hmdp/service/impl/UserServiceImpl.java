package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
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
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import javax.swing.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import com.hmdp.utils.RedisConstants;

import static com.hmdp.utils.RedisConstants.*;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>r
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    //发送验证码
    @Override
    public Result sendPhone(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone) ) {
            return Result.fail("手机号格式错误");
        }

        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        log.debug("code = " + code);
        //保存验证码到redis 两分钟
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
        //发送验证码
        log.debug("发送成功");
        //返回
        return Result.ok();
    }

    //用户登录
    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)){
            return Result.fail("手机号格式错误");
        }
        //从redis获取验证码
        String cashCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cashCode==null || !cashCode.equals(code)){
            return Result.fail("验证码错误");
        }
        //根据手机号查用户
        User user = query().eq("phone", loginForm.getPhone()).one();

        if (user == null) {
            //用户不存在，创建用户
            user = createUserWithPhone(loginForm.getPhone());
        }
        //保存用户信息到redis
        //创建token

        UUID uuid = UUID.randomUUID();
        String token = uuid.toString();
        //用redis中的hash结构存储
        UserDTO userDTO = BeanUtil.copyProperties(user,UserDTO.class);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((filedName,fieldValue)->fieldValue.toString())
                );
        String tokenKey = LOGIN_USER_KEY + token;

        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        //创建用户
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(8));
        //保存用户
        save(user);
        return user;
    }
}
