package com.hmdp.mapper;

import com.hmdp.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

/**
 * <p>
 *  Mapper 接口
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
public interface UserMapper extends BaseMapper<User> {

    @Select("select * from tb_user where phone = #{phone}")
    User queryByPhone(Object phone);

    @Insert("insert into tb_user (phone, password, nick_name, icon, create_time, update_time) " +
            "values (#{phone},#{password},#{nickName},#{icon},#{createTime},#{updateTime})")
    void addUser(User user);
}
