package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result queryList() {
        // 1. 定义Redis List的key
        String key = "cache:shop:type";

        // 2. 从Redis List中查询所有元素（0到-1表示查询全部）
        List<String> shopTypeJsonList = stringRedisTemplate.opsForList().range(key, 0, -1);

        // 3. 判断缓存是否存在（List非空且元素数>0）
        if (shopTypeJsonList != null && !shopTypeJsonList.isEmpty()) {
            // 4. 遍历Redis List中的JSON字符串，反序列化为ShopType对象
            List<ShopType> typeList = new ArrayList<>();
            for (String jsonStr : shopTypeJsonList) {
                ShopType shopType = JSONUtil.toBean(jsonStr, ShopType.class);
                typeList.add(shopType);
            }
            return Result.ok(typeList);
        }

        // 5. 缓存不存在，查询数据库
        List<ShopType> typeList = this.list(new LambdaQueryWrapper<ShopType>()
                .orderByAsc(ShopType::getSort));

        // 6. 数据库中无数据，返回错误
        if (typeList == null || typeList.isEmpty()) {
            return Result.fail("店铺类型不存在");
        }

        // 7. 数据库有数据，写入Redis List（逐个添加元素）
        // 先清空旧数据（避免脏数据，可选，根据业务场景）
        stringRedisTemplate.delete(key);
        for (ShopType shopType : typeList) {
            String jsonStr = JSONUtil.toJsonStr(shopType);
            // 向Redis List尾部添加元素
            stringRedisTemplate.opsForList().rightPush(key, jsonStr);
        }

        // 8. 设置Redis List的过期时间（注意：Redis List本身不支持过期，需给key设置过期）
        stringRedisTemplate.expire(key, 30, TimeUnit.MINUTES);

        return Result.ok(typeList);
        }
}
