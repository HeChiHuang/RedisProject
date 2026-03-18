package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.BeanUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
       Shop shop = queryWithMutex(id);
       if (shop == null) {
           return Result.fail("店铺不存在");
       }
        //返回店铺信息
        return Result.ok(shop);
    }

    private Shop queryWithMutex(Long id){
        //从redis查询商户
        String shopJson = stringRedisTemplate.opsForValue().get(CACHE_SHOP_KEY+id);
        if (StrUtil.isNotBlank(shopJson)) {
            //存在直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }
        //判断是否命中空值
        if (shopJson != null){
            return null;
        }
        String lockKey = LOCK_SHOP_KEY + id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //获取失败
            if (!isLock){
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            //成功获取锁
            shop = getById(id);
            //模拟重建延迟
            Thread.sleep(200);
            //MySQL
            //不存在，根据id查数据库
            if (shop == null){
                stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,"",CACHE_NULL_TTL, TimeUnit.MINUTES);
                return null;
            }
            //存在，写入reids
            stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(shop)
                    ,RedisConstants.CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            unlock(lockKey);
        }
        return shop;
    }

    private boolean tryLock(String lockKey){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String lockKey){
        Boolean flag = stringRedisTemplate.delete(lockKey);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null){
            return Result.fail("店铺id为空");
        }
        //更数据库
        updateById(shop);
        //删除缓存
        stringRedisTemplate.delete(CACHE_SHOP_KEY+shop.getId());

        return null;
    }
}
