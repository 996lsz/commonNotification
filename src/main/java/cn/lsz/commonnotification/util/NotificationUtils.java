package cn.lsz.commonnotification.util;

import cn.lsz.commonnotification.entity.NotificationMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author chin
 * @contact chenyan@afanticar.com
 * @since 2021/5/10/010
 */
public class NotificationUtils {

    private static ThreadLocal<List<NotificationMessage>> MESSAGE_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * user默认不传将使用注解上的通知人
     * @param msg
     */
    public static void notifyUser(String msg) {
        NotificationMessage message = NotificationMessage.builder().contactEnum(NotificationMessage.ContactEnum.USER).msg(msg).build();
        set(message);
    }

    /**
     * user默认不传将使用注解上的通知人
     * @param msg
     */
    public static void notifyUser(String msg, String... user) {
        NotificationMessage message = NotificationMessage.builder().contactEnum(NotificationMessage.ContactEnum.USER).msg(msg).contacts(Arrays.asList(user)).build();
        set(message);
    }

    /**
     * group默认不传将使用注解上的通知人
     * @param msg
     */
    public static void notifyGroup(String msg) {
        NotificationMessage message = NotificationMessage.builder().contactEnum(NotificationMessage.ContactEnum.GROUP).msg(msg).build();
        set(message);
    }

    /**
     * group默认不传将使用注解上的通知人
     * @param msg
     */
    public static void notifyGroup(String msg, String... group) {
        NotificationMessage message = NotificationMessage.builder().contactEnum(NotificationMessage.ContactEnum.GROUP).msg(msg).contacts(Arrays.asList(group)).build();
        set(message);
    }

    private static synchronized void set(NotificationMessage message){
        List<NotificationMessage> list = MESSAGE_THREAD_LOCAL.get();
        if(list == null){
            list = new ArrayList<>();
            MESSAGE_THREAD_LOCAL.set(list);
        }
        list.add(message);
    }

    public static List<NotificationMessage> get(){
        return MESSAGE_THREAD_LOCAL.get();
    }

    public static void remove(){
        MESSAGE_THREAD_LOCAL.remove();
    }
}
