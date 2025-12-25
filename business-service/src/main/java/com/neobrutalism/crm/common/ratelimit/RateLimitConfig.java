package com.neobrutalism.crm.common.ratelimit;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;

/**
 * Rate limiting configuration using Bucket4j with Redis backend
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "rate-limit.enabled", havingValue = "true")
@ConditionalOnMissingBean(ProxyManager.class)
public class RateLimitConfig {

    @Bean
    public ProxyManager<String> proxyManager(RedisConnectionFactory connectionFactory) {
        log.info("Initializing Redis-based rate limiting with Bucket4j");

        if (connectionFactory instanceof LettuceConnectionFactory lettuceFactory) {
            String host = lettuceFactory.getStandaloneConfiguration().getHostName();
            int port = lettuceFactory.getStandaloneConfiguration().getPort();
            String password = lettuceFactory.getPassword();

            String uri = (password == null || password.isEmpty())
                    ? String.format("redis://%s:%d", host, port)
                    : String.format("redis://:%s@%s:%d", password, host, port);

            RedisClient redisClient = RedisClient.create(uri);
            StatefulRedisConnection<String, byte[]> connection = redisClient.connect(
                    RedisCodec.of(StringCodec.UTF8, ByteArrayCodec.INSTANCE)
            );

            return LettuceBasedProxyManager.builderFor(connection)
                    .build();
        }

        throw new IllegalStateException("RedisConnectionFactory must be LettuceConnectionFactory for rate limiting");
    }
}
