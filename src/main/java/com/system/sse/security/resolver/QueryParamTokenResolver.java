package com.system.sse.security.resolver;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

//@Component
public class QueryParamTokenResolver implements TokenResolver {
    private static final String PARAM_NAME = "token";

    @Override
    public String resolve(HttpServletRequest request) {
        String token = request.getParameter(PARAM_NAME);

        return StringUtils.hasText(token) ? token : null;
    }
}