package top.futurenotfound.mqtt.client.client;

import org.eclipse.paho.mqttv5.client.*;
import org.eclipse.paho.mqttv5.common.MqttException;
import org.eclipse.paho.mqttv5.common.MqttSubscription;

public class MqttV5Client extends MqttClient {

    public MqttV5Client(String serverURI, String clientId, MqttClientPersistence persistence) throws MqttException {
        super(serverURI, clientId, persistence);
    }

    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners) throws MqttException {
        if (topicFilters.length != qos.length) {
            throw new MqttException(MqttClientException.REASON_CODE_UNEXPECTED_ERROR);
        }

        MqttSubscription[] subscriptions = new MqttSubscription[topicFilters.length];
        for (int i = 0; i < topicFilters.length; ++i) {
            subscriptions[i] = new MqttSubscription(topicFilters[i], qos[i]);
        }

        return this.subscribe(subscriptions, messageListeners);
    }
}
