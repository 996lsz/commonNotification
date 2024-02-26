package cn.lsz.commonnotification.test;

import cn.lsz.commonnotification.annotation.NotificationAnnotation;
import cn.lsz.commonnotification.util.NotificationUtils;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.validation.ValidationException;
import java.util.List;


@RestController
@RequestMapping("/commonnotification")
@NotificationAnnotation(msg = "方法没有该注解时使用顶部通知", userContacts = {"通知人"}, groupContacts = {"通知群"})
public class Controller {

    /**
     * 支持配置文件方法配置联系人，冒号右边为默认值
     企微号取自aftc.wx_employee  user_id
     */
    private final String USER_CONTACT = "${test.user_contact:替换配置文件内容}";


    /**
     * 测试类注解通知
     * 当方法没有@NotificationAnnotation时将使用类注解属性
     */
    @GetMapping("/test1")
    public void test1() {
        int i = 1/0;
    }

    /**
     * 测试方法注解通知
     * 当方法有@NotificationAnnotation时将使用方法注解属性，类注解属性将全部覆盖
     *
     */
    @GetMapping("/test2")
    @NotificationAnnotation(msg = "test2", userContacts = USER_CONTACT)
    public void test2() {
        int i = 1/0;
    }

    /**
     * 测试方法内通知
     * 当方法有@NotificationAnnotation时将使用方法注解属性，类注解属性将全部覆盖
     *
     */
    @GetMapping("/test3")
    public void test3() {
        //1、当notify方法不传参时，则使用注解参数作为联系人
        NotificationUtils.notifyUser("方法内通知1");
        NotificationUtils.notifyGroup("方法内通知1");
        //同一个联系人的多个通知会打包整合成一次通知
        NotificationUtils.notifyUser("方法内通知1-1");
        //2、也可以通过传参指定联系人
/*		NotificationUtils.notifyUser("方法内通知2","其他人");
		NotificationUtils.notifyGroup("方法内通知2", "微信群组");*/
        int i = 1/0;
        //3、程序中断后会将前面的通知发出，通知3中断无法发出
        NotificationUtils.notifyUser("方法内通知3");
    }

    /**
     * 测试忽略异常
     * exclude指定类型异常不做通知
     *
     */
    @GetMapping("/test4")
    @NotificationAnnotation(userContacts = USER_CONTACT, exclude = ArithmeticException.class)
    public void test4() {
        int i = 1/0;
    }

    /**
     * 测试忽略参数
     * 设置ignore = true可以忽略该方法不做通知，使用场景为设置了类注解，但是想忽略类内某些方法告警
     *
     */
    @GetMapping("/test5")
    @NotificationAnnotation(ignore = true)
    public void test5() {
        int i = 1/0;
    }


    /**
     * 测试通知参数通知
     * 企微通知将带上入参
     *
     * request Example
     * {
     * 	"id": "111",
     * 	"listIds": ["222", "333"],
     * 	"innerClass": [{
     * 			"innerId": "444-1"
     *                },
     *        {
     * 			"innerId": "444-2",
     * 			"innerListIds": ["555", "666"]
     *        }
     * 	]
     * }
     */
    @PostMapping("/test6")
    @NotificationAnnotation(msg = "当前项目名称(可通过yml配置):%s ; 方法参数:%s ;\n %s ;\n %s ;\n %s\n",
            msgArgs = {"${spring.application.name}", "#{param.getId()}", "#{param.getListIds()}", "#{param.getInnerClass().getInnerId()}", "#{param.getInnerClass().getInnerListIds()}"},
            userContacts = USER_CONTACT)
    public void test6(@RequestBody TestEntity param) {
        System.out.println(param);
		/*param.getId();
		param.getListIds();
		List<TestEntity.TestClass> innerClassList = param.getInnerClass();
		for (TestEntity.TestClass innerClass : innerClassList) {
			innerClass.getInnerId();
			innerClass.getInnerListIds();
		}*/
        int i = 1/0;
    }

    /**
     * 测试类注解通知
     * 简化版，只想保障程序出错就通知，通知效果如下:
     *
     *  【DEV cmd-monitor 2021-09-17 11:28:12】com.afanticar.cmdmonitor.controller.monitor.demo.DemoController test7 发生异常
     *  异常信息:ArithmeticException: / by zero
     */
    @GetMapping("/test7")
    @NotificationAnnotation(userContacts = USER_CONTACT)
    public void test7() {
        int i = 1/0;
    }

    @Data
    static class TestEntity {

        private String id;

        private List<String> listIds;

        private List<TestClass> innerClass;

        @Data
        public static class TestClass{

            private String innerId;

            private List<String> innerListIds;
        }

    }

    /**
     * 测试异常熔断机制
     * 熔断机制为30分钟内出现5次同一类型异常则触发告警，后续限流5分钟只能告警一次
     * 即：如果某一瞬间出现大量告警，那么只能收到5条告警，如果程序仍持续报错，那么每间隔5分钟后可以再收到一次告警内容
     */
    public static int count = new Integer("0");

    @GetMapping("/test8")
    @NotificationAnnotation(userContacts = USER_CONTACT)
    public void test8() {
        try {
            count ++;
            int i = 1 / 0;
        }finally {
            if(count > 15){
                throw new NullPointerException();
            }else if(count > 7){
                throw new ValidationException();
            }
        }
    }
}
