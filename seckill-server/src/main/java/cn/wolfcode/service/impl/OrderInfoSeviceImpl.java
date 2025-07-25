package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.*;
import cn.wolfcode.mapper.OrderInfoMapper;
import cn.wolfcode.mapper.PayLogMapper;
import cn.wolfcode.mapper.RefundLogMapper;
import cn.wolfcode.redis.SeckillRedisKey;
import cn.wolfcode.seckillserver.cache.TwoLevelCacheService;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.service.ISeckillProductService;
import cn.wolfcode.util.IdGenerateUtil;
import cn.wolfcode.web.feign.IntegralFeignApi;
import cn.wolfcode.web.feign.PayFeignApi;
import cn.wolfcode.web.msg.SeckillCodeMsg;
import com.alibaba.fastjson.JSON;
import io.seata.spring.annotation.GlobalTransactional;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import cn.wolfcode.seckillserver.cache.BloomFilterService;

/**
 * Created by wolfcode-lanxw
 */
@Service
public class OrderInfoSeviceImpl implements IOrderInfoService {
    @Autowired
    private ISeckillProductService seckillProductService;
    @Autowired
    private OrderInfoMapper orderInfoMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private PayFeignApi payFeignApi;
    @Autowired
    private PayLogMapper payLogMapper;
    @Autowired
    private RefundLogMapper refundLogMapper;
    @Autowired
    private IntegralFeignApi integralFeignApi;



    @Override
    public OrderInfo findByPhoneAndSeckillId(String phone, Long seckillId) {
        return orderInfoMapper.findByPhoneAndSeckillId(phone,seckillId);
    }

    @Override
    @Transactional
    public OrderInfo doSeckill(String phone, SeckillProductVo seckillProductVo) {
        //4.扣减数据库库存
        int effectCount = seckillProductService.decrStockCount(seckillProductVo.getId());
        if(effectCount==0){
            //说明update语句执行结果影响行数为0,说明stock_count>0条件不满足,说明没有库存了.抛出异常提示用户.
            throw new BusinessException(SeckillCodeMsg.SECKILL_STOCK_OVER);
        }
        //5.创建秒杀订单
        OrderInfo orderInfo = createOrderInfo(phone,seckillProductVo);

        return orderInfo;
    }

    @Override
    public OrderInfo findByOrderNo(String orderNo) {
        //从Redis中查询
        String orderHashKey = SeckillRedisKey.SECKILL_ORDER_HASH.getRealKey("");
        String objStr = (String) redisTemplate.opsForHash().get(orderHashKey, orderNo);
        return JSON.parseObject(objStr,OrderInfo.class);
        
    }

    private OrderInfo createOrderInfo(String phone, SeckillProductVo seckillProductVo) {
        OrderInfo orderInfo = new OrderInfo();
        BeanUtils.copyProperties(seckillProductVo,orderInfo);
        orderInfo.setUserId(Long.parseLong(phone));//用户ID
        orderInfo.setCreateDate(new Date());//订单创建时间
        orderInfo.setDeliveryAddrId(1L);//收货地址ID
        orderInfo.setSeckillDate(seckillProductVo.getStartDate());//秒杀商品的日期
        orderInfo.setSeckillTime(seckillProductVo.getTime());//秒杀场次
        orderInfo.setOrderNo(String.valueOf(IdGenerateUtil.get().nextId()));//订单编号
        orderInfo.setSeckillId(seckillProductVo.getId());//秒杀的ID
        orderInfoMapper.insert(orderInfo);
        return orderInfo;
    }
    @Override
    @Transactional
    public void cancelOrder(String orderNo) {
        System.out.println("超时取消订单逻辑开始....");
        OrderInfo orderInfo = orderInfoMapper.find(orderNo);
        //判断订单是否处于未付款状态
        if(OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())){
            //修改订单状态
            int effectCount = orderInfoMapper.updateCancelStatus(orderNo, OrderInfo.STATUS_TIMEOUT);
            if(effectCount==0){
                return;
            }
            //真实库存回补
            seckillProductService.incrStockCount(orderInfo.getSeckillId());
            //预库存回补
            seckillProductService.syncStockToRedis(orderInfo.getSeckillTime(),orderInfo.getSeckillId());
        }
        System.out.println("超时取消订单逻辑结束....");
    }
    @Value("${pay.returnUrl}")
    private String returnUrl;
    @Value("${pay.notifyUrl}")
    private String notifyUrl;
    @Override
    public Result<String> payOnline(String orderNo) {
        //1. 根据订单号查询订单对象
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        // 2. 校验订单状态，必须是未支付
        if(OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())){
            // 3. 构造支付参数 PayVo
            PayVo vo = new PayVo();
            vo.setOutTradeNo(orderNo);
            vo.setTotalAmount(String.valueOf(orderInfo.getSeckillPrice()));
            vo.setSubject(orderInfo.getProductName());
            vo.setBody(orderInfo.getProductName());
            vo.setReturnUrl(returnUrl);
            vo.setNotifyUrl(notifyUrl);
            // 4. 远程调用支付服务（pay-service），发起支付
            Result<String> result = payFeignApi.payOnline(vo);
            return result;
        }
        return Result.error(SeckillCodeMsg.PAY_STATUS_CHANGE);
    }

    @Override
    public int changePayStatus(String orderNo, Integer status, int payType) {
        return orderInfoMapper.changePayStatus(orderNo,status,payType);
    }

    @Override
    public void refundOnline(OrderInfo orderInfo) {
        RefundVo vo = new RefundVo();
        vo.setOutTradeNo(orderInfo.getOrderNo());
        vo.setRefundAmount(String.valueOf(orderInfo.getSeckillPrice()));
        vo.setRefundReason("不想要了.");
        Result<Boolean> result = payFeignApi.refund(vo);
        if(result==null || result.hasError() || !result.getData()){
            throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
        }
        orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(),OrderInfo.STATUS_REFUND);
    }

    @Override
    @GlobalTransactional
    public void payIntegral(String orderNo) {
        OrderInfo orderInfo = this.findByOrderNo(orderNo);
        if(OrderInfo.STATUS_ARREARAGE.equals(orderInfo.getStatus())){
            //处于未支付状态
            //插入支付日志记录
            //1. 插入日志,保证幂等性
            PayLog log = new PayLog();
            log.setOrderNo(orderNo);
            log.setPayTime(new Date());
            log.setTotalAmount(String.valueOf(orderInfo.getIntergral()));
            log.setPayType(OrderInfo.PAYTYPE_INTERGRAL);
            payLogMapper.insert(log);
            //2. 远程调用积分服务完成积分扣减
            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(orderInfo.getUserId());
            vo.setValue(orderInfo.getIntergral());
            //调用积分服务
            Result result = integralFeignApi.decrIntegral(vo);
            if(result==null || result.hasError()){
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            //3. 修改订单状态
            int effectCount = orderInfoMapper.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_INTERGRAL);
            if(effectCount==0){
                throw new BusinessException(SeckillCodeMsg.PAY_ERROR);
            }
        }
    }

    @Override
    @GlobalTransactional
    public void refundIntegral(OrderInfo orderInfo) {
        if(OrderInfo.STATUS_ACCOUNT_PAID.equals(orderInfo.getStatus())){
            //添加退款记录
            RefundLog log = new RefundLog();
            log.setOrderNo(orderInfo.getOrderNo());
            log.setRefundAmount(orderInfo.getIntergral());
            log.setRefundReason("不要了");
            log.setRefundTime(new Date());
            log.setRefundType(OrderInfo.PAYTYPE_INTERGRAL);
            refundLogMapper.insert(log);
            //远程调用服务增加积分
            //调用积分服务
            OperateIntergralVo vo = new OperateIntergralVo();
            vo.setUserId(orderInfo.getUserId());
            vo.setValue(orderInfo.getIntergral());
            Result result = integralFeignApi.incrIntegral(vo);
            if(result==null || result.hasError()){
                throw new BusinessException(SeckillCodeMsg.INTERGRAL_SERVER_ERROR);
            }
            //修改订单状态
            int effectCount = orderInfoMapper.changeRefundStatus(orderInfo.getOrderNo(),OrderInfo.STATUS_REFUND);
            if(effectCount==0){
                throw new BusinessException(SeckillCodeMsg.REFUND_ERROR);
            }
        }
    }
}
