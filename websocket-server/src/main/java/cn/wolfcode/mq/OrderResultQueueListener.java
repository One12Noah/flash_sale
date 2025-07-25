package cn.wolfcode.mq;

import cn.wolfcode.ws.OrderWSServer;
import com.alibaba.fastjson.JSON;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

import javax.websocket.Session;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Created by wolfcode-lanxw
 */
@Component
@RocketMQMessageListener(consumerGroup = "OrderResultGroup",topic = MQConstants.ORDER_RESULT_TOPIC)
public class OrderResultQueueListener implements RocketMQListener<OrderMQResult> {
    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        System.out.println("消费消息:"+JSON.toJSONString(orderMQResult));
        //找到客户端
        //若Session未建立，最多重试3次（每次间隔100ms）
        Session session = null;
        int count = 3;
        while(count-->0){
            session = OrderWSServer.clients.get(orderMQResult.getToken());
            if(session!=null){
                //说明已经拿到了,发送消息
                try {
                    session.getBasicRemote().sendText(JSON.toJSONString(orderMQResult));
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return ;
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
                //通知不到用户,对整体的业务没有影响.用户还可以在《我的订单》看到是否创建了订单.
            }
        }
    }
}
