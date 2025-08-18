package com.system.auth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component("queryParamResolver")
public class QueryParamTokenResolver implements TokenResolver {
    private static final String PARAM_NAME = "token";

    @Override
    public String resolve(HttpServletRequest request) {
        String token = request.getParameter(PARAM_NAME);

        return StringUtils.hasText(token) ? token : null;
    }
}