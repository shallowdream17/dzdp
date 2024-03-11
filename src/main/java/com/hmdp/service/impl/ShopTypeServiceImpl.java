package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        String shopTypeJson = stringRedisTemplate.opsForValue().get(RedisConstants.SHOP_TYPE_KEY);
        if(shopTypeJson!=null&&!shopTypeJson.isEmpty()){
            List<ShopType> shopTypeList = JSONUtil.toList(JSONUtil.parseArray(shopTypeJson),ShopType.class);
            return Result.ok(shopTypeList);
        }
        List<ShopType> typeList = query().orderByAsc("sort").list();
        if(typeList==null){
            return Result.fail("店铺列表不存在!");
        }
        stringRedisTemplate.opsForValue().set(RedisConstants.SHOP_TYPE_KEY,JSONUtil.toJsonStr(typeList),RedisConstants.SHOP_TYPE_TTL, TimeUnit.MINUTES);
        return Result.ok(typeList);
    }
}
