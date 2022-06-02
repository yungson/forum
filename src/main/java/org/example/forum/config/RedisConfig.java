package org.example.forum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory){
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        // seralization method for key
        template.setKeySerializer(RedisSerializer.string());
        // seralization method for value
        template.setValueSerializer(RedisSerializer.json());
        // for hash key
        template.setHashKeySerializer(RedisSerializer.string());
        // for hash value
        template.setHashValueSerializer(RedisSerializer.json());
        template.afterPropertiesSet(); // set the configuration to effective
        return template;
    }
}
