package com.youshangdache.common.aspect;

import com.youshangdache.common.annotation.Login;
import com.youshangdache.common.constant.RedisConstant;
import com.youshangdache.common.execption.GuiguException;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.common.util.AuthContextHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * @author QRH
 * @date 2024/7/17 23:10
 * @description 登录切面类
 */
@Component
@Aspect
public class LoginAspect {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Around(value = "execution(* com.youshangdache.*.controller.*.*(..)) && @annotation(login)")
    public Object login(ProceedingJoinPoint proceedingJoinPoint, Login login) throws Throwable {
        //1 获取request对象
        ServletRequestAttributes sra = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (sra == null) {
            throw new GuiguException(ResultCodeEnum.ILLEGAL_REQUEST);
        }
        HttpServletRequest request = sra.getRequest();
        //2 从请求头中获取token，查询redis
        String token = request.getHeader("token");
        //3 查询redis对应用户ID
        String customerId = stringRedisTemplate.opsForValue().get(RedisConstant.USER_LOGIN_KEY_PREFIX + token);

        if (!StringUtils.hasText(customerId)) {
            throw new GuiguException(ResultCodeEnum.LOGIN_AUTH);
        }
        //4 查询redis对应用户ID，把用户id放到threadlocal中
        AuthContextHolder.setUserId(Long.parseLong(customerId));
        return proceedingJoinPoint.proceed(); //删除threadLocal键值应该在网关的拦截器中才对
    }
}
