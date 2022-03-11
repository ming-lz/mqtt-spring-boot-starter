package top.futurenotfound.mqtt.client.env;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Shared subscription type
 *
 * @author liuzhuoming
 */
@AllArgsConstructor
@Getter
public enum MqttSharedSubscriptionType {
    QUEUE("$queue/"),
    SHARE("$share/"),
    ;

    private final String prefix;
}
