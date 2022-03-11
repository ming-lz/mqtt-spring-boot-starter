package top.futurenotfound.mqtt.client;

/**
 * mqtt exception
 *
 * @author liuzhuoming
 */
public class MqttV5ClientException extends RuntimeException {

    private static final long serialVersionUID = -8386867880112662775L;

    public MqttV5ClientException() {
        super();
    }

    public MqttV5ClientException(String message) {
        super(message);
    }

    public MqttV5ClientException(String message, Throwable cause) {
        super(message, cause);
    }

    public MqttV5ClientException(Throwable cause) {
        super(cause);
    }
}
