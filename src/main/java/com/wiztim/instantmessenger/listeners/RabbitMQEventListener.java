package com.wiztim.instantmessenger.listeners;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Envelope;
import com.wiztim.instantmessenger.service.SessionService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Data
@Slf4j
public class RabbitMQEventListener {

    @Autowired
    private SessionService sessionService;

    // https://www.rabbitmq.com/event-exchange.html
    // used for listening to queue.created and queue.deleted
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "instantmessenger.event.queue", durable = "true"),
            exchange = @Exchange(value = "amq.rabbitmq.event", internal = "true", type = "topic"),
            key = "queue.*")
    )
    public void receiveEventMessage(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) {
        log.info("Received <" + consumerTag + ">");
//        Long id = Long.valueOf(message.get("id"));
//        log.info("Message processed...");
//
//        String event = envelope.getRoutingKey();
//        Map<String, Object> headers = properties.getHeaders();
//        String name = headers.get("name").toString();
//        String vhost = headers.get("vhost").toString();
//
//
//        if (event.equals("queue.created")) {
//            boolean durable = (Boolean) headers.get("durable");
//            String durableString = durable ? " (durable)" : " (transient)";
//            log.info("Created: " + name + " in " + vhost + durableString);
//        }
//        else /* queue.deleted is the only other possibility */ {
//            log.info("Deleted: " + name + " in " + vhost);
//        }
        // TODO handle user status updates based on created/deleted events
    }
}
