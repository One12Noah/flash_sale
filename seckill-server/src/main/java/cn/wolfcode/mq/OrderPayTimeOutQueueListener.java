package cn.wolfcode.mq;

import cn.wolfcode.service.IOrderInfoService;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created by wolfcode-lanxw
 */
@Component
@RocketMQMessageListener(consumerGroup = "OrderPayTimeOutGroup",topic = MQConstant.ORDER_PAY_TIMEOUT_TOPIC)
public class OrderPayTimeOutQueueListener implements RocketMQListener<OrderMQResult> {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Override
    public void onMessage(OrderMQResult orderMQResult) {
        System.out.println("超时取消订单逻辑");
        //取消订单
        orderInfoService.cancelOrder(orderMQResult.getOrderNo());
    }
}
