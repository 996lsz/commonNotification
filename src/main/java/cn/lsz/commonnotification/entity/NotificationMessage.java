package cn.lsz.commonnotification.entity;

import lombok.*;

import java.util.List;

/**
 * description
 * 
 * @author LSZ 2021/07/21 15:56
 * @contact 648748030@qq.com
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationMessage {

	public enum ContactEnum {
		/**
		 * 个人
		 */
		USER,
		/**
		 * 群组
		 */
		GROUP,
		;

	}

	private ContactEnum contactEnum;

	private List<String> contacts;

	private String msg;


}
