package com.system.auth.resolver;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class CompositeTokenResolver implements TokenResolver {
    private final List<TokenResolver> resolvers;

    public String resolve(HttpServletRequest request) {
        for (TokenResolver resolver : resolvers) {
            String token = resolver.resolve(request);

            if (token != null) {
                return token;
            }
        }

        return null;
    }
}