package cn.wolfcode.mq;

import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.domain.SeckillProductVo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

/**
 * Created by wolfcode-lanxw
 */
@Component
@RocketMQMessageListener(consumerGroup = "penddingGroup",topic = MQConstant.ORDER_PEDDING_TOPIC)
public class OrderPeddingQueueListener implements RocketMQListener<OrderMessage> {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private RocketMQTemplate rocketMQTemplate;
    @Override
    public void onMessage(OrderMessage orderMessage) {
        OrderMQResult result = new OrderMQResult();
        result.setToken(orderMessage.getToken());
        String tag;
        try{
            // 1. 查询商品信息（从缓存）
            SeckillProductVo vo = seckillProductService.findFromCache(orderMessage.getTime(), orderMessage.getSeckillId());
            // 2. 创建订单（扣减数据库库存、写订单表等）
            OrderInfo orderInfo = orderInfoService.doSeckill(String.valueOf(orderMessage.getUserPhone()), vo);
            result.setOrderNo(orderInfo.getOrderNo());
            tag = MQConstant.ORDER_RESULT_SUCCESS_TAG;
            //发送延时消息
            // 发送时间3s钟，3s没到达就会抛出异常
            // 延时10min

            // 这个地方也可以使用redis
            // 3. 发送延时消息，处理支付超时
            Message<OrderMQResult> message = MessageBuilder.withPayload(result).build();
            rocketMQTemplate.syncSend(MQConstant.ORDER_PAY_TIMEOUT_TOPIC,message,3000,MQConstant.ORDER_PAY_TIMEOUT_DELAY_LEVEL);
        }catch (Exception e) {
            e.printStackTrace();
            result.setCode(SeckillCodeMsg.SECKILL_ERROR.getCode());
            result.setMsg(SeckillCodeMsg.SECKILL_ERROR.getMsg());
            result.setTime(orderMessage.getTime());
            result.setSeckillId(orderMessage.getSeckillId());
            tag = MQConstant.ORDER_RESULT_FAIL_TAG;
        }
        // 4. 发送订单结果消息（成功/失败）
        rocketMQTemplate.syncSend(MQConstant.ORDER_RESULT_TOPIC+":"+tag,result);
    }
}
