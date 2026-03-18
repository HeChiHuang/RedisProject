package com.hmdp.utils;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.hmdp.dto.UserDTO;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class RefreshTokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    public RefreshTokenInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        System.out.println(">>> [Order 0] Refresh Interceptor 启动！");
        //redis获取用户
        String token = request.getHeader("authorization");
        System.out.println("拦截器收到的Token: " + token);
        //判断token是否为空
        if(StrUtil.isBlank(token)){
            return true;
        }

        Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(RedisConstants.LOGIN_USER_KEY + token);
        System.out.println("Redis查询到的Map: " + userMap);
        //判断用户是否存在
        if (userMap.isEmpty()) {
            return true;
        }
        //基于token获取用户
        UserDTO user = BeanUtil.fillBeanWithMap(userMap,new UserDTO(),false);

        //存在则保存用户
        UserHolder.saveUser((UserDTO) user);
        //刷新token存续时间
        stringRedisTemplate.expire(RedisConstants.LOGIN_USER_KEY + token,RedisConstants.LOGIN_USER_TTL, TimeUnit.MINUTES);
        //放行
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        UserHolder.removeUser();
    }
}
