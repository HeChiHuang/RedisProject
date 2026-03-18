package Test;

import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;

class MyTest{
    @Resource
    static StringRedisTemplate stringRedisTemplate;

    public static void main(String[] args) {
        try {
            // 写入一个带过期时间的数据，防止污染数据库
            stringRedisTemplate.opsForValue().set("test:check", "success");

            // 获取数据
            String result = stringRedisTemplate.opsForValue().get("test:check");

            System.out.println("--------------------------------");
            System.out.println("Redis 连接状态: 成功！");
            System.out.println("读取到的测试值: " + result);
            System.out.println("--------------------------------");

        } catch (Exception e) {
            System.err.println("--------------------------------");
            System.err.println("Redis 连接失败！错误原因: " + e.getMessage());
            System.err.println("--------------------------------");
            e.printStackTrace();
        }
    }
    void test(){
        stringRedisTemplate.opsForValue().set("test:check", "success");
    }
}