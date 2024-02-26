package cn.lsz.commonnotification.job;

import cn.lsz.commonnotification.aop.NotificationAspect;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 定时清理本地频控缓存
 *
 * @author LSZ 2022/09/06 18:19
 * @contact 648748030@qq.com
 */
@Component
public class CleanCacheJob {

    private static ScheduledExecutorService pool = Executors.newScheduledThreadPool(1);

    /**
     * 每30分钟清理一遍频率缓存
     */
    @PostConstruct
    public void refreshTopicRuleLocalCache() {
        pool.scheduleAtFixedRate(() -> NotificationAspect.cleanFrequency(), 10, 30, TimeUnit.MINUTES);
    }


}
