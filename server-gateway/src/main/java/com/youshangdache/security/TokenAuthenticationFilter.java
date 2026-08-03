package com.youshangdache.security;


import com.youshangdache.common.result.Result;
import com.youshangdache.common.result.ResultCodeEnum;
import com.youshangdache.common.util.AuthContextHolder;
import com.youshangdache.common.util.ResponseUtil;
import com.youshangdache.model.entity.system.SysUser;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 身份验证过滤器
 */
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    private RedisTemplate redisTemplate;

    private static final String ADMIN_LOGIN_KEY_PREFIX = "admin:login:";
    private static final AntPathMatcher ANT_PATH_MATCHER = new AntPathMatcher();

    private static final List<String> WHITE_LIST = List.of(
            "/securityLogin/login",
            "/swagger-resources/**",
            "/webjars/**",
            "/v3/**",
            "/doc.html",
            "/favicon.ico"
    );

    public TokenAuthenticationFilter(RedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException {
        String uri = request.getRequestURI();
        if (isWhitelisted(uri)) {
            chain.doFilter(request, response);
            return;
        }

        UsernamePasswordAuthenticationToken authentication = getAuthentication(request);
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        } else {
            ResponseUtil.out(response, Result.build(null, ResultCodeEnum.PERMISSION));
        }
    }

    /**
     * 判断uri是否在白名单中
     * @param uri 请求URI
     * @return true表示在白名单中，false表示不在白名单中
     */
    private boolean isWhitelisted(String uri) {
        for (String pattern : WHITE_LIST) {
            if (ANT_PATH_MATCHER.match(pattern, uri)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 获取用户的所有权限
     * @param request
     * @return
     */
    private UsernamePasswordAuthenticationToken getAuthentication(HttpServletRequest request) {
        String token = request.getHeader("token");
        SysUser sysUser = (SysUser) redisTemplate.opsForValue().get(ADMIN_LOGIN_KEY_PREFIX + token);
        if (sysUser == null) {
            return null;
        }

        AuthContextHolder.setUserId(sysUser.getId());
        List<SimpleGrantedAuthority> authorities = Collections.emptyList();
        if (sysUser.getUserPermsList() != null && !sysUser.getUserPermsList().isEmpty()) {
            authorities = sysUser.getUserPermsList()
                    .stream()
                    .filter(code -> StringUtils.hasText(code.trim()))
                    .map(code -> new SimpleGrantedAuthority(code.trim()))
                    .collect(Collectors.toList());
        }

        return new UsernamePasswordAuthenticationToken(sysUser.getUsername(), null, authorities);
    }
}
