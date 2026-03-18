package com.hmdp.config;

import com.hmdp.utils.LoginInterceptor;
import com.hmdp.utils.RefreshTokenInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Configuration
public class MvcConfig implements WebMvcConfigurer {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
                System.out.println("超级拦截器我他妈已经启动");
                return true;
            }
        }).order(-1);
        //拦截所有请求并且刷新登录状态
        registry.addInterceptor(new RefreshTokenInterceptor(stringRedisTemplate))
                .addPathPatterns("/**")
                .order(0);
        //登录拦截器
        registry.addInterceptor(new LoginInterceptor())
                .excludePathPatterns(
                        "/user/code",      // 发送验证码（必须放行）
                        "/user/login",     // 登录接口（必须放行）
                        "/blog/hot",       // 首页热榜（无需登录）
                        "/shop/**",        // 商户查看（无需登录）
                        "/shop-type/**",   // 分类查看（无需登录）
                        "/upload/**",      // 静态资源
                        "/voucher/**"      // 优惠券查看（无需登录）
                )
                .order(1);
    }
}
