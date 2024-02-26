package cn.lsz.commonnotification.service;

import com.alibaba.fastjson.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;


/**
 * @author chin
 * @contact chenyan@afanticar.com
 * @since 2021/5/10/010
 */
@Service
public class NotificationService {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());


    @Async("commonNotifyThreadPool")
    public void sendTextNotification(String toUser, String textContent) {
        //单人告警实现
        LOGGER.info("告警到负责人:{},告警内容:{}",toUser, textContent);
    }


    @Async("commonNotifyThreadPool")
    public void sendGroupTextNotification(String chatid, String textContent) {
        //群告警实现
        LOGGER.info("告警到群组:{},告警内容:{}",chatid, textContent);
    }

    
    @Async("commonNotifyThreadPool")
    public void notify(String[] userContacts, String[] groupContacts, String msg) {
        //群告警实现
        LOGGER.info("告警到负责人:{}, 告警到群组:{},告警内容:{}",JSONObject.toJSONString(userContacts), JSONObject.toJSONString(groupContacts), msg);
    }


}
