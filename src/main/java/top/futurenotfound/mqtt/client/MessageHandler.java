package top.futurenotfound.mqtt.client;

import org.eclipse.paho.mqttv5.common.MqttMessage;

import java.util.function.Consumer;

/**
 * message handle function interface
 *
 * @author liuzhuoming
 */
@FunctionalInterface
public interface MessageHandler extends Consumer<MqttMessage> {
}
