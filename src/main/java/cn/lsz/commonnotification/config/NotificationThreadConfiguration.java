package cn.lsz.commonnotification.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author lsz
 */
@Configuration
public class NotificationThreadConfiguration {

    @Bean(name = "commonNotifyThreadPool")
    public ThreadPoolTaskExecutor commonNotifyThreadPool() {
        ThreadPoolTaskExecutor threadPool = new ThreadPoolTaskExecutor();
        //设置核心线程数
        threadPool.setCorePoolSize(1);
        //设置最大线程数
        threadPool.setMaxPoolSize(5);
        //线程池所使用的缓冲队列
        threadPool.setQueueCapacity(1000);
        //等待任务在关机时完成--表明等待所有线程执行完
        threadPool.setWaitForTasksToCompleteOnShutdown(false);
        // 等待时间 （默认为0，此时立即停止），并没等待xx秒后强制停止
        threadPool.setAwaitTerminationSeconds(60);
        threadPool.setKeepAliveSeconds(30);
        //  线程名称前缀
        threadPool.setThreadNamePrefix("common-notify-async-");

        // 线程池满时丢弃任务
        threadPool.setRejectedExecutionHandler(new ThreadPoolExecutor.DiscardPolicy());
        // 初始化线程
        threadPool.initialize();
        return threadPool;
    }


}