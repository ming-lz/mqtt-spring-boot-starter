package top.futurenotfound.mqtt.client;

import lombok.AllArgsConstructor;
import org.eclipse.paho.mqttv5.client.MqttClient;
import org.eclipse.paho.mqttv5.client.MqttConnectionOptions;
import org.eclipse.paho.mqttv5.client.persist.MemoryPersistence;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import top.futurenotfound.mqtt.client.env.MqttVersion;

import java.nio.charset.StandardCharsets;

/**
 * mqtt auto configuration
 *
 * @author liuzhuoming
 */
@Configuration
@EnableConfigurationProperties({MqttClientProperties.class})
@AllArgsConstructor
@Order(1)
public class MqttAutoConfiguration {

    private final MqttClientProperties properties;
    private final MqttClientCallback mqttClientCallback;

    @Bean
    @ConditionalOnMissingBean(MqttClient.class)
    public MqttClient mqttClient() {
        try {
            if (MqttVersion.V5.equals(properties.getVersion())) {
                MqttClient client = new MqttV5Client(properties.getServer(), properties.getClientId(), new MemoryPersistence());
                MqttConnectionOptions options = new MqttConnectionOptions();
                options.setCleanStart(properties.isCleanStart());
                options.setUserName(properties.getUsername());
                options.setPassword(properties.getPassword().getBytes(StandardCharsets.UTF_8));
                options.setConnectionTimeout(properties.getConnectionTimeout());
                options.setKeepAliveInterval(properties.getKeepAliveInterval());
                client.setCallback(mqttClientCallback);
                client.connect(options);
                return client;
            }
            throw new MqttV5ClientException("Not support Mqtt version 3.1");
        } catch (MqttException e) {
            e.printStackTrace();
            throw new MqttV5ClientException(properties.getServer() + " connect fail.", e);
        }
    }
}
