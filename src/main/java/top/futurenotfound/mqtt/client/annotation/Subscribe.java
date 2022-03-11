package top.futurenotfound.mqtt.client.annotation;

import top.futurenotfound.mqtt.client.SubscribeHandler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * must implements {@code top.futurenotfound.mqtt.client.SubHandler}
 *
 * @author liuzhuoming
 * @see SubscribeHandler
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Subscribe {
    String[] topics();

    int qos() default 0;
}
