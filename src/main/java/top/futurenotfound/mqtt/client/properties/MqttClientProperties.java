package top.futurenotfound.mqtt.client.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import top.futurenotfound.mqtt.client.env.MqttVersion;

/**
 * properties
 *
 * @author liuzhuoming
 */
@ConfigurationProperties(prefix = "mqtt")
@Data
public class MqttClientProperties {
    private MqttVersion version = MqttVersion.V5;

    private String server;
    private String clientId;
    private String username;
    private String password;
    private boolean cleanStart;
    private int connectionTimeout;
    private int keepAliveInterval;
}
