package cn.lsz.commonnotification.properties;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * description
 *
 * @author LSZ 2021/07/06 14:15
 * @contact 648748030@qq.com
 */
@Setter
@Configuration
public class NotificationProperties {

	@Getter
	@Value("${spring.profiles.active:local}")
	private String springProfilesActive;

	/**
	 * 	区分环境（根据目前已有的配置项进行优先选择）
	 */
	@Value("${common-notification.application.env:}")
	private String notifyApplicationEnv;


	/**
	 * 区分项目名称
	 */
	@Value("${common-notification.application.name:}")
	private String notifyApplicationName;

	@Value("${spring.application.name:}")
	private String springApplicationName;

	/**
	 * 忽略告警的包路径
	 */
	@Getter
	@Value("${common-notification.exclude:}")
	private List<String> exclude;

	/**
	 * 获取当前环境
	 * 优先级 notifyApplicationEnv > springProfilesActive
	 * @return
	 */
	public String getEnv(){
		if(StringUtils.isNotBlank(notifyApplicationEnv)){
			return notifyApplicationEnv;
		}
		if(StringUtils.isNotBlank(springProfilesActive)){
			return springProfilesActive;
		}
		return "";
	}

	/**
	 * 获取当前项目名称
	 * 优先级 notifyApplicationName > springApplicationName > 容器环境变量PROJECT_NAME
	 * @return
	 */
	public String getApplicationName(){
		if(StringUtils.isNotBlank(notifyApplicationName)){
			return notifyApplicationName;
		}
		if(StringUtils.isNotBlank(springApplicationName)){
			return springApplicationName;
		}

		//优先取运维配置的项目名称
		String projectName = System.getenv("PROJECT_NAME");
		if(projectName != null){
			String application = projectName.split("::")[1];
			return application;
		}

		return "";
	}

}
