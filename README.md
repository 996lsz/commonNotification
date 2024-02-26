# commonNotification

项目开发中经常会出现这种问题：
DEV代码测的没有问题，过一段时间就出问题了，最终找到的原因是上游数据出现改动导致链式反应
程序兼容处理做的不够，未能处理意料之外的情况
为了能够及时响应异常，单单依靠个人主动性去检查日志效率是不够的，需要借助工具能够使得开发人员都够被动及时接收到代码出现的异常        

该组件通过自定义注解方式，当被注解代理的类/方法出现异常，执行NotificationService进行异常通知告警
之前项目是通过接入企微进行告警的，这里把企微相关代码去掉了
![B(NCQZ{V786HE0449PN6M0I](https://github.com/996lsz/commonNotification/assets/49548423/2b4b5428-6d54-43de-adf8-52df8ebefa98)
