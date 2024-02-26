package cn.lsz.commonnotification.aop;

import cn.lsz.commonnotification.annotation.NotificationAnnotation;
import cn.lsz.commonnotification.entity.NotificationMessage;
import cn.lsz.commonnotification.properties.NotificationProperties;
import cn.lsz.commonnotification.service.NotificationService;
import cn.lsz.commonnotification.util.NotificationUtils;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.collections.map.MultiValueMap;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * description
 *
 * @author LSZ 2019/10/15 15:29
 * @contact 648748030@qq.com
 */
@Aspect
@Component
public class NotificationAspect {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private NotificationService sendNotificationService;

    @Autowired
    private Environment environment;

	@Autowired
	private NotificationProperties notificationProperties;

    //记录告警次数map ：  Map<异常class.method, Map<yyyyMMddHHmm,告警次数>>
    private static Map<String, Map<String, AtomicInteger>> FREQUENCY_MAP = new HashMap<>();
	//记录异常次数map：  Map<异常class.method, Map<yyyyMMddHHmm,异常次数>>
	private static Map<String, Map<String, AtomicLong>> FREQUENCY_COUNT_MAP = new HashMap<>();

	/**
	 * 判断是否属于忽略告警的包路径
	 */
	private static Map<String, Boolean> IS_EXCLUDE_PACKAGE_MAP = new HashMap<>();
	/**
	 * 路径匹配工具
	 */
	private AntPathMatcher PACKAGE_URL_MATCHER = new AntPathMatcher(File.separator);

	/**
	 * 清理熔断缓存
	 */
	public static void cleanFrequency(){
		Date date = new Date();
		Long yyyyMMddHHmm = Long.parseLong(DateFormatUtils.format(date, "yyyyMMddHHmm"));
		String yyyyMMdd = DateFormatUtils.format(date, "yyyyMMdd");

		for (Map<String, AtomicInteger> value : FREQUENCY_MAP.values()) {
			for (String key : value.keySet()) {
				if(yyyyMMddHHmm - Long.parseLong(key) > 30){
					value.remove(key);
				}
			}
		}

		for (Map<String, AtomicLong> value : FREQUENCY_COUNT_MAP.values()) {
			for (String key : value.keySet()) {
				if(yyyyMMdd.compareTo(key) > 0){
					value.remove(key);
				}
			}
		}

	}

	@Around("@within(cn.lsz.commonnotification.annotation.NotificationAnnotation) || @annotation(cn.lsz.commonnotification.annotation.NotificationAnnotation)")
	public Object aroundLog(ProceedingJoinPoint joinpoint) throws Throwable {
		NotificationAnnotation notifyAnnotation = getAnnotation(joinpoint);

		Set<Class> includeClassSet = getThrowableClassMap(notifyAnnotation.include());
		Set<Class> excludeClassSet = getThrowableClassMap(notifyAnnotation.exclude());
		boolean ignore = notifyAnnotation.ignore();
		Boolean excludePackage = isExcludePackage(joinpoint);
		try {
			Object proceed = joinpoint.proceed();
			return proceed;
		}catch (Exception e){
			if(!ignore && !excludePackage) {
				String[] userContacts = replacePropertiesValues(notifyAnnotation.userContacts());
				String[] groupContacts = replacePropertiesValues(notifyAnnotation.groupContacts());
				if (checkContain(includeClassSet, e)) {
					String msg = buildMsg(joinpoint, notifyAnnotation, e);
					sendNotificationService.notify(userContacts, groupContacts, msg);
					throw e;
				} else if (checkContain(excludeClassSet, e)) {
					throw e;
				}
				//判断告警频率是否达到熔断机制
				if(notifyAnnotation.exceptionBreak()){
					if(checkFrequency(joinpoint, e)){
						String msg = buildMsg(joinpoint, notifyAnnotation, e) + "(今日已出现该异常" + getExceptionCount(joinpoint, e) +"次)";
						sendNotificationService.notify(userContacts, groupContacts, msg);
					}
				}else {
					String msg = buildMsg(joinpoint, notifyAnnotation, e);
					sendNotificationService.notify(userContacts, groupContacts, msg);
				}
			}
			throw e;
		}finally {
			//NotificationUtils补充通知
			if(!ignore && !excludePackage) {
				List<NotificationMessage> notificationMessages = NotificationUtils.get();
				if (notificationMessages != null && notificationMessages.size() > 0) {
					dealExtraContacts(notificationMessages, notifyAnnotation);
				}
			}
			NotificationUtils.remove();
		}
	}

	/**
	 * 处理补充通知
	 * @param notificationMessages
	 * @param notifyAnnotation
	 */
	private void dealExtraContacts(List<NotificationMessage> notificationMessages, NotificationAnnotation notifyAnnotation) {
		try{
			Map<String, NotificationMessage.ContactEnum> contactTypeMap = new HashMap<>();
			MultiValueMap multiValueMap = new MultiValueMap();
			//消息分区
			for (NotificationMessage message : notificationMessages) {
				List<String> contacts = message.getContacts();
				//如果没有指定联系人，那么取注解上的联系人
				if(contacts == null){
					if(message.getContactEnum() == NotificationMessage.ContactEnum.USER){
						for (String userContact : notifyAnnotation.userContacts()) {
							userContact = replacePropertiesValue(userContact);
							contactTypeMap.put(userContact, message.getContactEnum());
							multiValueMap.put(userContact, message.getMsg());
						}
					}
					if(message.getContactEnum() == NotificationMessage.ContactEnum.GROUP){
						for (String groupContact : notifyAnnotation.groupContacts()) {
							groupContact = replacePropertiesValue(groupContact);
							contactTypeMap.put(groupContact, message.getContactEnum());
							multiValueMap.put(groupContact, message.getMsg());
						}
					}
				}else {
					for (String contact : contacts) {
						contact = replacePropertiesValue(contact);
						contactTypeMap.put(contact, message.getContactEnum());
						multiValueMap.put(contact, message.getMsg());
					}
				}
			}
			//企微通知
			for (Object contact : multiValueMap.keySet()) {
				NotificationMessage.ContactEnum contactEnum = contactTypeMap.get(contact);
				//组装消息
				StringBuilder msg = new StringBuilder();
				Collection<String> collection = multiValueMap.getCollection(contact);
				collection.forEach(item -> msg.append(item).append("\n"));
				//根据通知人类型通知
				if(contactEnum == NotificationMessage.ContactEnum.USER){
					sendNotificationService.sendTextNotification(contact.toString(), msg.toString());
				}
				if(contactEnum == NotificationMessage.ContactEnum.GROUP){
					sendNotificationService.sendGroupTextNotification(contact.toString(), msg.toString());
				}
			}

		}finally {
			//NotificationUtils.remove();
		}
	}

	/**
	 * 优先获取方法上注解，再获取类头部注解
	 * @param joinpoint
	 * @return
	 * @throws NoSuchMethodException
	 */
	private NotificationAnnotation getAnnotation(ProceedingJoinPoint joinpoint) throws NoSuchMethodException {
		// 获取当前拦截方法的对象
		MethodSignature msig = (MethodSignature) joinpoint.getSignature();
		Method targetMethod = joinpoint.getTarget().getClass().getDeclaredMethod(msig.getName(), msig.getMethod().getParameterTypes());
		// 获取当前方法注解中的值
		NotificationAnnotation notifyAnnotation = targetMethod.getAnnotation(NotificationAnnotation.class);
		// 如果方法没有注解，则获取类上的注解
		if (notifyAnnotation == null) {
			notifyAnnotation = joinpoint.getTarget().getClass().getAnnotation(NotificationAnnotation.class);
		}
		return notifyAnnotation;
	}

    /**
     * 构建错误通知消息，默认带上【env 项目名】
     * 默认【env 项目名】class method 发生未知异常
     * @param joinPoint
     * @param notifyAnnotation
     * @return
     * @throws NoSuchMethodException
     */
    private String buildMsg(ProceedingJoinPoint joinPoint, NotificationAnnotation notifyAnnotation, Exception e) throws NoSuchMethodException {
        //默认前缀
    	String defaultPrefix = getDefaultPrefix();
        //默认后缀（异常信息）
	    String defaultSuffix = String.format("\n 异常信息:%s", ExceptionUtils.getMessage(e));

        String msg = notifyAnnotation.msg();
        //方法的相关内容
        Signature sig = joinPoint.getSignature();
        MethodSignature msig = (MethodSignature) sig;
        Object target = joinPoint.getTarget();
        Method currentMethod = target.getClass().getMethod(msig.getName(), msig.getParameterTypes());
        try {
            //默认抛出异常错误
            if (msg.isEmpty()) {
                String defaultMsg = String.format("%s %s 发生异常", target.getClass().getName(), msig.getName());
                return defaultPrefix + defaultMsg + defaultSuffix;
            } else {
                //自定义异常
                //将参数名跟对象关联成MAP
                Map<String, Object> parametersMap = parametersMap(joinPoint.getArgs(), currentMethod.getParameters());

                String[] args = notifyAnnotation.msgArgs();
                //判断参数类型
                for (int i = 0; i < args.length; i++) {
                    String arg = args[i];
                    //环境变量
                    if (arg.startsWith("${") && arg.endsWith("}")) {
                        arg = environment.getProperty(arg.substring(2, arg.length() - 1));
                        args[i] = arg == null ? "" : arg;
                    }
                    //入参
                    if (arg.startsWith("#{") && arg.endsWith("}")) {
						arg = arg.substring(2, arg.length() - 1);
                        String[] split = arg.split("\\.");
                        String argName = split[0];
                        //argName = argName.substring(2);
                        Object parameterArg = parametersMap.get(argName);
                        //执行方法链
                        List<String> temp = new ArrayList<>(Arrays.asList(split));
                        temp.remove(0);
                        args[i] = invokeMethodInvocation(parameterArg, temp);
                    }
                }
                String resultMsg = String.format(msg, args);
                return defaultPrefix + resultMsg + defaultSuffix;
            }
        }catch (Exception ie){
            String error = String.format("%s %s 通知注解使用出现异常", target.getClass().getName(), msig.getName());
            LOGGER.error(error, ie);
            return error;
        }
    }

    private Map<String, Object> parametersMap(Object[] args, Parameter[] parameters){
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < parameters.length; i++) {
            map.put(parameters[i].getName(), args[i]);
        }
        return map;
    }

    private Boolean checkContain(Set<Class> set, Exception e){
        if(set.contains(e.getClass())){
            return true;
        }
        return false;
    }

    private Set<Class> getThrowableClassMap(Class<? extends Throwable>[] throwables){
        Set<Class> ThrowableClassSet = new HashSet<>();
        for (Class<? extends Throwable> excludeClass : throwables) {
            ThrowableClassSet.add(excludeClass);
        }
        return ThrowableClassSet;
    }


	private String invokeMethodInvocation(Object arg, List<String> iterator) throws Exception {
		StringBuilder builder = new StringBuilder();
		if(iterator.size() == 0){
			if(arg != null){
				if(arg instanceof String){
					return arg.toString();
				}
				String json = JSONObject.toJSONString(arg);
				return json;
			}
		} else {
			String methodName = iterator.get(0);
			methodName = methodName.replaceAll("\\(\\).*","");
			iterator.remove(0);
			if(arg instanceof Iterable){
				Iterable iterable = (Iterable) arg;
				List<String> temp = new ArrayList<>();
				for (Object o : iterable) {
					ArrayList<String> nextMethodIterator = new ArrayList<>(Arrays.asList(iterator.toArray(new String[]{})));
					Object nextArg = MethodUtils.invokeMethod(o, methodName);
					temp.add(invokeMethodInvocation(nextArg, nextMethodIterator));
				}
				builder.append(JSONObject.toJSONString(temp));
			} else{
				ArrayList<String> nextMethodIterator = new ArrayList<>(Arrays.asList(iterator.toArray(new String[]{})));
				Object nextArg = MethodUtils.invokeMethod(arg, methodName);
				builder.append(invokeMethodInvocation(nextArg, nextMethodIterator));
			}
		}
		String temp = builder.toString();
		String unescapeStr = StringEscapeUtils.unescapeJava(temp);
		return unescapeStr;
	}


    private String[] replacePropertiesValues(String[] args){
    	String[] result = new String[args.length];
	    for (int i = 0; i < args.length; i++) {
		    String arg = args[i];
		    //环境变量
		    result[i] = replacePropertiesValue(arg);

	    }
	    return result;
    }

	private String replacePropertiesValue(String arg){
		if (arg.startsWith("${") && arg.endsWith("}")) {
			String propertyName = arg.substring(2, arg.length() - 1);
			//判断是否有默认值
			String[] split = propertyName.split(":");
			arg = environment.getProperty(split[0]);
			if(arg == null && split.length == 2){
				arg = split[1];
			}
		}
		return arg == null ? "" : arg;
	}


	/**
	 * 判断告警频率是否达到熔断机制
	 * 1、缓存时间内告警次数 <= 5，允许告警
	 * 2、缓存时间内告警次数 > 5，如果间隔上一次告警超过5分钟允许一次告警
	 * 相当于Exception短时间内最多出现3次告警，然后限流每5分钟只允许出现一次告警直至缓存被清理，重复以上熔断机制
	 *
	 * 解除熔断需要定时任务清理缓存
	 *
	 * @return
	 * @param joinpoint
	 * @param e
	 */
	private static Boolean checkFrequency(ProceedingJoinPoint joinpoint, Exception e){
		//这里应该是锁抛异常的方法对象的，但是需要往下掰，偷懒就锁整个类对象了(性能影响比较大，最好不要出现一个类下多个方法报错)
		Object target = joinpoint.getTarget();
		//上了锁内部方法就不做多线程处理了，预期是想做内部都是用juc + cas操作完成的
		synchronized (target){
			String methodExceptionKey = getMethodExceptionKey(joinpoint, e);
			incrementExceptionCount(methodExceptionKey);
			String dateKey = DateFormatUtils.format(new Date(), "yyyyMMddHHmm");
			//如果存在记录，判断是否达到熔断
			if (FREQUENCY_MAP.containsKey(methodExceptionKey)) {
				Map<String, AtomicInteger> stringAtomicIntegerMap = FREQUENCY_MAP.get(methodExceptionKey);
				int count = stringAtomicIntegerMap.values().stream().mapToInt(item -> item.intValue()).sum();
				if(count < 5){
					if(stringAtomicIntegerMap.containsKey(dateKey)){
						stringAtomicIntegerMap.get(dateKey).incrementAndGet();
					}else {
						stringAtomicIntegerMap.put(dateKey, new AtomicInteger(1));
					}
					return true;
				}else {
					List<Long> collect = stringAtomicIntegerMap.keySet().stream().map(item -> Long.parseLong(item)).sorted(Comparator.reverseOrder()).collect(Collectors.toList());
					//缓存时间内告警次数 > 5，如果间隔上一次告警超过5分钟允许一次告警
					if(Long.parseLong(dateKey) - collect.get(0) >= 5){
						stringAtomicIntegerMap.put(dateKey, new AtomicInteger(1));
						return true;
					}else {
						return false;
					}
				}
			} else {
				//如果不存在记录，说明还没有达到熔断阈值
				Map countMap = new HashMap<>();
				FREQUENCY_MAP.put(methodExceptionKey, countMap);
				countMap.put(dateKey, new AtomicInteger(1));
				return true;
			}
		}
	}

	private Long getExceptionCount(ProceedingJoinPoint joinpoint, Exception e){
		String dateKey = DateFormatUtils.format(new Date(), "yyyyMMdd");
		String methodExceptionKey = getMethodExceptionKey(joinpoint, e);
		AtomicLong atomicLong = FREQUENCY_COUNT_MAP.get(methodExceptionKey).get(dateKey);
		if(atomicLong == null){
			return 0L;
		}
		return atomicLong.get();
	}

	private static void incrementExceptionCount(String methodExceptionKey){
		String dateKey = DateFormatUtils.format(new Date(), "yyyyMMdd");
		if (!FREQUENCY_COUNT_MAP.containsKey(methodExceptionKey)) {
			FREQUENCY_COUNT_MAP.put(methodExceptionKey, new HashMap<>());
		}
		Map<String, AtomicLong> stringAtomicLongMap = FREQUENCY_COUNT_MAP.get(methodExceptionKey);
		if(!stringAtomicLongMap.containsKey(dateKey)){
			stringAtomicLongMap.put(dateKey, new AtomicLong((0)));
		}
		stringAtomicLongMap.get(dateKey).incrementAndGet();
	}

	private static String getMethodExceptionKey(ProceedingJoinPoint joinpoint, Exception e){
		return joinpoint.getSignature().getDeclaringTypeName() + "." + joinpoint.getSignature().getName() + "(" + e.getClass().toString() + ")";
	}

	/**
	 * 判断告警路径是否在忽略列表中
	 * @return
	 */
	private Boolean isExcludePackage(ProceedingJoinPoint joinPoint){
		try {
			Object target = joinPoint.getTarget();
			String name = target.getClass().getName();
			//方法的相关内容
			Signature sig = joinPoint.getSignature();
			MethodSignature msig = (MethodSignature) sig;

			name = name + "." + msig.getName();
			Boolean result = IS_EXCLUDE_PACKAGE_MAP.get(name);
			if (result != null) {
				return result;
			} else {
				//解析
				List<String> excludes = notificationProperties.getExclude();
				if (CollectionUtils.isEmpty(excludes)) {
					IS_EXCLUDE_PACKAGE_MAP.put(name, false);
					return false;
				} else {
					for (String exclude : excludes) {
						boolean match = PACKAGE_URL_MATCHER.match(exclude, name);
						if (match) {
							IS_EXCLUDE_PACKAGE_MAP.put(name, true);
							return true;
						}
					}
					IS_EXCLUDE_PACKAGE_MAP.put(name, false);
					return false;
				}
			}
		}catch (Exception e){
			LOGGER.error("common-notification未知异常，请联系组件开发人员", e);
			return false;
		}
	}


	public String getDefaultPrefix(){
		String format = DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss");
		String defaultPrefix = String.format("【%s %s %s】", notificationProperties.getEnv(), notificationProperties.getApplicationName(), format);
		return defaultPrefix;
	}
}
