package com.system.sse.flux;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.time.Instant;

@ExtendWith(SpringExtension.class)
public class MonoTest {

    @Test
    @DisplayName("Mono Test")
    void test() {
        final Instant now = Instant.now();
        System.out.println("now = " + now);
    }

}
