package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.mapper.UserMapper;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RegexUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.nio.file.CopyOption;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.*;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public Result sendCode(String phone, HttpSession session) {
         //    校验手机号
        if(RegexUtils.isPhoneInvalid(phone)){
            //        如果不符合，返回错误信息
            return Result.fail("lanxuan验证不成功");
        }
//        符合，生成验证码
        String code = RandomUtil.randomNumbers(6);
//        把验证码保存到redis
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);
//        发送验证码
        log.debug("发送验证码成功，验证码：{}",code);
        return Result.ok();

    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
//        1校验手机号
        String phone = loginForm.getPhone();
        if(RegexUtils.isPhoneInvalid(phone)){
            //        如果不符合，返回错误信息
            return Result.fail("手机号格式错误");
        }

//     2校验验证码,从redis获取验证
        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        String code = loginForm.getCode();
        if(cacheCode==null || !cacheCode.equals(code)){
            //        3不一致，报错
            return Result.fail("验证码错误");
        }


//        4一致，根据手机号查询用户 select * from tb_user where phone=?
        User user = query().eq("phone", phone).one();
//        5判断用户是否存在
        if(user ==null){
            //        6不存在，创建并赋值
            user=createUserWithPhone(phone);
        }
        //        7保存用户信息到session中
//        随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
//        存储
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);

        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO,new HashMap<>(),
                CopyOptions.create().setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName,fieldValue)->fieldValue.toString()));


        String tokenKey =LOGIN_USER_KEY +token;
        stringRedisTemplate.opsForHash().putAll(tokenKey,userMap);

        stringRedisTemplate.expire(tokenKey,LOGIN_USER_TTL,TimeUnit.MINUTES);


        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {

        User user=new User();
        user.setPhone(phone);
        user.setNickName("user"+RandomUtil.randomString(10));
        return null;
    }
}
