package cn.lsz.commonnotification.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * description
 * 
 * @author LSZ 2021/07/06 17:53
 * @contact 648748030@qq.com
 */
@Configuration
@EnableAsync
@ConditionalOnProperty(value = "common-notification.open", matchIfMissing = true)
@ComponentScan("cn.lsz.commonnotification")
public class NotificationConfig {
    
}
