package com.hmdp.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpSession;
import java.util.Random;

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

    @Override
    public Result sendPhone(String phone, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(phone) ) {
            return Result.fail("手机号格式错误");
        }

        //生成验证码
        String code = RandomUtil.randomNumbers(6);
        log.debug("code = " + code);
        //保存验证码到session
        session.setAttribute("code", code);

        //发送验证码
        log.debug("发送成功");
        //返回
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        //校验手机号
        if (RegexUtils.isPhoneInvalid(loginForm.getPhone())) {
            return Result.fail("手机号格式错误");
        }
        //校验验证码
        Object cashCode = session.getAttribute("code");
        String code = loginForm.getCode();
        if(cashCode==null || !cashCode.toString().equals(code)){
            return Result.fail("验证码错误");
        }
        //根据手机号查用户
        User user = query().eq("phone", loginForm.getPhone()).one();
        if (user == null) {
            //用户不存在，创建用户
            user = createUserWithPhone(loginForm.getPhone());
        }
        //保存用户信息到session
        session.setAttribute("user", user);
        return Result.ok();
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
