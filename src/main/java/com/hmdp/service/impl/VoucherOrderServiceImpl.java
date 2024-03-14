package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.ISeckillVoucherService;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.aop.framework.AopContext;
import org.springframework.beans.factory.annotation.Autowired;
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
    private ISeckillVoucherService iSeckillVoucherService;

    @Autowired
    private RedisIdWorker redisIdWorker;

    @Override
    //@Transactional
    public Result seckillVoucher(Long voucherId) {
        //1.查询优惠券
        SeckillVoucher voucher = iSeckillVoucherService.getById(voucherId);
        //2.判断秒杀是否开始
        if(voucher.getBeginTime().isAfter(LocalDateTime.now())){
            return Result.fail("优惠券秒杀尚未开始!");
        }
        //3.判断秒杀是否已经结束
        if(voucher.getEndTime().isBefore(LocalDateTime.now())){
            return Result.fail("优惠券秒杀已经结束!");
        }
        //4.判断库存是否充足
        if(voucher.getStock()<1){
            return Result.fail("优惠券已经被抢光了!");
        }

        Long userId = UserHolder.getUser().getId();
        synchronized (userId.toString().intern()) {
            IVoucherOrderService proxy = (IVoucherOrderService) AopContext.currentProxy();
            return proxy.createVoucherOrder(voucherId);
        }
    }

    @Transactional
    public Result createVoucherOrder(Long voucherId){
        //实现一人一单
        Long userId = UserHolder.getUser().getId();
        // 查询订单
        int count = query().eq("user_id", userId).eq("voucher_id", voucherId).count();
        // 判断是否存在
        if(count>0){
            return Result.fail("用户已经购买过一次!");
        }
        //5.扣减库存
        boolean success = iSeckillVoucherService.update()
                .setSql("stock = stock -1")
                .eq("voucher_id",voucherId)
                .gt("stock",0)
                .update();
        if(!success){
            return Result.fail("优惠券已经被抢光了!");
        }
        //6.创建订单
        VoucherOrder voucherOrder = new VoucherOrder();
        //6.1订单id
        long orderId = redisIdWorker.nextId("order");
        voucherOrder.setId(orderId);
        //6.2用户id
        //Long userId = UserHolder.getUser().getId();
        voucherOrder.setUserId(userId);
        //6.3代金券id
        voucherOrder.setVoucherId(voucherId);
        save(voucherOrder);
        //7.返回订单id
        return Result.ok(orderId);
    }
}
