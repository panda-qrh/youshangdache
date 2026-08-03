package com.qrh.youshangdache.common.security;

import com.qrh.youshangdache.common.result.Result;
import com.qrh.youshangdache.common.result.ResultCodeEnum;
import com.qrh.youshangdache.common.util.ResponseUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;


@Component
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) {

        ResponseUtil.out(response, Result.build(null, ResultCodeEnum.ACCOUNT_ERROR));
    }
}
