package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSON;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.RedisData;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;

import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
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

   @Resource
   private CacheClient cacheClient;
   @Override


   public Result queryById(Long id) {
//          缓存穿透
//       Shop shop = cacheClient.queryWithPassThrough
//               (CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);

//       互斥锁解决缓存击穿
//       Shop shop = queryWithMutex(id);

//       用逻辑过期解决
       Shop shop = cacheClient.queryWithLogicalExpire
               (CACHE_SHOP_KEY,id,Shop.class,this::getById,CACHE_SHOP_TTL,TimeUnit.MINUTES);
       if(shop ==null){
           return Result.fail("店铺不存在");
       }
//       返回
       return Result.ok(shop);
   }



    //       用逻辑过期解决缓存击穿
//    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);
//    public Shop queryWithLogicalExpire(Long id){
//        String key=CACHE_SHOP_KEY + id;
//        //1从redis缓存中查询该用户
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2判断是否存在
//        if (StrUtil.isBlank(shopJson)) {
//             //3不存在，直接返回null，去mysql
//            return null;
//        }
//        //TODO
//        //4命中，将json反序列化为对象
//        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
//        JSONObject data = (JSONObject) redisData.getData();
//        Shop shop = JSONUtil.toBean(data, Shop.class);
//        LocalDateTime expireTime = redisData.getExpireTime();
//
//        //5判断是否过期
//
//        if (expireTime.isAfter(LocalDateTime.now())) {
//            //5.1.未过期，返回店铺信息
//            return shop;
//        }
//
//        //5.2已过期，需要缓存重建
//
//        String lockKey = RedisConstants.LOCK_SHOP_KEY+id;
//        //6缓存重建
//        //6.1获取互斥锁
//        boolean isLock=tryLock(lockKey);
//
//        //6。2判断是否获取成功
//        if(isLock){
//            //6.3成功，开启独立线程，实现缓存重建
//            CACHE_REBUILD_EXECUTOR.submit(()->{
//                try {
//                    this.saveShop2Redis(id,20L);
//                } catch (Exception e) {
//                    throw new RuntimeException(e);
//                } finally {unLock(lockKey);
//                }
//            });
//        }
//
//        //6.4返回过期的商铺信息
//
//
//
//           return shop;
//    }
    //       互斥锁解决缓存击穿
    /*public Shop queryWithMutex(Long id){
        String key=CACHE_SHOP_KEY + id;
        //从redis缓存中查询该用户
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        //判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            //存在，直接返回

            return JSONUtil.toBean(shopJson, Shop.class);
        }

//       判断命中的是否为空值
        if(shopJson != null){
            return null;
        }

        //4实现缓存重建
        //4.1获取互斥锁
        String lockKey="lock:shop:"+id;
        Shop shop = null;
        try {
            boolean isLock = tryLock(lockKey);
            //4.2判断是否获取成功
            if (!isLock) {
                //4.3失败，休眠并重试
                Thread.sleep(50);
             return    queryWithMutex(id);
            }


            //4.4成功，根据id查询数据库
            shop = getById(id);
            Thread.sleep(200);

            //不存在，报错
            if (shop==null) {
    //           将空值写入redis
                stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);

                return null;
            }

            //存在，将商品数据写入缓存，返回商铺信息
            stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            //释放互斥锁
            unLock(lockKey);
        }
        return shop;
    }*/
    //          缓存穿透
  /* public Shop queryWithPassThrough(Long id){
       String key=CACHE_SHOP_KEY + id;
       //从redis缓存中查询该用户
       String shopJson = stringRedisTemplate.opsForValue().get(key);
       //判断是否存在
       if (StrUtil.isNotBlank(shopJson)) {
           //存在，直接返回

           return JSONUtil.toBean(shopJson, Shop.class);
       }

//       判断命中的是否为空值
       if(shopJson != null){
           return null;
       }

       //不存在，根据id查询数据库
       Shop shop = getById(id);

       //不存在，报错
       if (shop==null) {
//           将空值写入redis
           stringRedisTemplate.opsForValue().set(key,"",CACHE_NULL_TTL, TimeUnit.MINUTES);

           return null;
       }
       //存在，将商品数据写入缓存，返回商铺信息
       stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),CACHE_SHOP_TTL, TimeUnit.MINUTES);
       return shop;
   }*/
//   //        设置互斥锁
//   private boolean tryLock(String key){
//       Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", 10, TimeUnit.SECONDS);
//       return BooleanUtil.isTrue(flag);
//   }
// //释放锁
//   private void unLock(String key){
//       stringRedisTemplate.delete(key);
//   }
//
//   //添加有逻辑过期时间对象的方法
//   public void saveShop2Redis(Long id,Long expireSeconds) throws InterruptedException {
//       Shop shop = getById(id);
//         Thread.sleep(200);
//
//       RedisData redisData = new RedisData();
//       redisData.setData(shop);
//       redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
//
//       stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
//   }

    @Override
    @Transactional
    public Result update(Shop shop) {

       Long id=shop.getId();
        if (id==null) {


            return Result.fail("店铺不能为空");
        }

        updateById(shop);

        stringRedisTemplate.delete(CACHE_SHOP_KEY+id);
        return Result.ok();
    }
}
