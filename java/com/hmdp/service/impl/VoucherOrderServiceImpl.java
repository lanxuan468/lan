package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IVoucherService;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {

    @Resource
    private ISeckillVoucherService seckillVoucherService;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Override
    public Result seckillVoucher(Long voucherId) {
        //1查询优惠劵
        SeckillVoucher voucher = seckillVoucherService.getById(voucherId);
        //2判断秒杀是否开始
        if (voucher.getBeginTime().isAfter(LocalDateTime.now())) {
            //秒杀尚未开始
            return Result.fail("秒杀尚未开始");
        }
        //3判断秒杀是否结束
        if (voucher.getEndTime().isBefore(LocalDateTime.now())) {
            return Result.fail("秒杀已经结束");
        }
        //4判断库存是否充足
        if (voucher.getStock()<1) {
            return Result.fail("库存不足");
        }

        Long userId = UserHolder.getUser().getId();
        synchronized(userId.toString().intern()) {
            IVoucherService proxy = (IVoucherService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public  Result createVoucherOrder(Long voucherId) {
        //5一人一单
        Long userId = UserHolder.getUser().getId();


            Integer count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
            if (count > 0) {
                return Result.fail("用户已经购买过一次");
            }
            //6扣减库存

            boolean success = seckillVoucherService.update()
                    .setSql("stock= stock-1")//set stock=stock-1
                    .eq("voucher_id", voucherId).gt("stock", 0)//where id=? and stock=?
                    .update();
            if (!success) {
                //扣减失败
                return Result.fail("库存不足");
            }

            //7创建订单

            VoucherOrder voucherOrder = new VoucherOrder();
            //7。1从之前写的生成全局唯一id的方法，生成订单id并写入对象的属性
            Long orderId = redisIdWorker.nextId("order");
            voucherOrder.setId(orderId);
            //7.2用户id
            voucherOrder.setUserId(userId);
            //7.3代金卷id
            voucherOrder.setVoucherId(voucherId);
            save(voucherOrder);
            //返回对象

            return Result.ok(orderId);

    }
    }
