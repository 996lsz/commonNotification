package cn.lsz.commonnotification.annotation;

import java.lang.annotation.*;

/**
 * 统一入参，输出，异常日志处理注解
 *
 * @author LSZ 2020/07/21 15:29
 * @contact 648748030@qq.com
 */
@Inherited
@Target({ ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotificationAnnotation {

    /**
     * 支持String.format格式
     * @return
     */
    String msg() default "";

    /**
     * msg里需要填充的参数，可支持配置文件${xxx.xxx}, 方法参数组合#{message.getMsgID()}
     * 目前尚不支持带参数的方法，如JSONObject.getString("xxx")
     * @return
     */
    String[] msgArgs() default {};

    /**
     * 单人通知名单,可支持配置文件${xxx.xxx}
     * @return
     */
    String[] userContacts() default {};

    /**
     * 群组通知名单,可支持配置文件${xxx.xxx}
     * @return
     */
    String[] groupContacts() default {};

    /**
     * 需要通知的异常，默认全部
     * @return
     */
    Class<? extends Throwable>[] include() default {};

    /**
     * 忽略通知的异常
     * @return
     */
    Class<? extends Throwable>[] exclude() default {};

    /**
     * 是否忽略告警
     * @return
     */
    boolean ignore() default false;

    /**
     * 是否执行异常熔断机制（对Exeption异常执行熔断机制，限制由于异常出现的告警数，不影响NotificationUtils的告警）
     * @return
     */
    boolean exceptionBreak() default true;

}
