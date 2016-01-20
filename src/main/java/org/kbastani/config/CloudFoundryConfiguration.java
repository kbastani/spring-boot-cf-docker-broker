package org.kbastani.config;

import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.cloud.Cloud;
import org.springframework.cloud.CloudFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;

/**
 * Configuration which creates deployers that deploy on Cloud Foundry.
 * Can be used either when running <i>in</i> Cloud Foundry, or <i>targeting</i> Cloud Foundry.
 *
 * @author Eric Bottard
 * @author Mark Fisher
 * @author Thomas Risberg
 * @author Ilayaperumal Gopinathan
 */
@Configuration
@Import(CloudFoundryDeployerConfiguration.class)
public class CloudFoundryConfiguration {

    @Profile("cloud")
    @AutoConfigureBefore(RedisAutoConfiguration.class)
    protected static class RedisConfig {

        @Bean
        public Cloud cloud(CloudFactory cloudFactory) {
            return cloudFactory.getCloud();
        }

        @Bean
        public CloudFactory cloudFactory() {
            return new CloudFactory();
        }

        @Bean
        RedisConnectionFactory redisConnectionFactory(Cloud cloud) {
            return cloud.getSingletonServiceConnector(RedisConnectionFactory.class, null);
        }
    }

}
