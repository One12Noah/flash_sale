package cn.wolfcode.web.controller;


import cn.wolfcode.common.web.Result;
import cn.wolfcode.domain.OrderInfo;
import cn.wolfcode.service.IOrderInfoService;
import cn.wolfcode.web.feign.PayFeignApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

/**
 * Created by lanxw
 */
@RestController
@RequestMapping("/orderPay")
@RefreshScope
public class OrderPayController {
    @Autowired
    private IOrderInfoService orderInfoService;
    @Autowired
    private PayFeignApi payFeignApi;
    @RequestMapping("/pay")
    public Result<String> pay(String orderNo,Integer type){
        if(OrderInfo.PAYTYPE_ONLINE.equals(type)){
            //在线支付
            return orderInfoService.payOnline(orderNo);
        }else{
            //积分支付
            orderInfoService.payIntegral(orderNo);
            return Result.success();
        }
    }
    @RequestMapping("/refund")
    public Result<String> refund(String orderNo){
        OrderInfo orderInfo = orderInfoService.findByOrderNo(orderNo);
        if(OrderInfo.PAYTYPE_ONLINE.equals(orderInfo.getPayType())){
            //在线退款
            orderInfoService.refundOnline(orderInfo);
        }else{
            //积分退款
            orderInfoService.refundIntegral(orderInfo);
        }
        return Result.success();
    }
    //异步回调
    @RequestMapping("/notifyUrl")
    public String notifyUrl(@RequestParam Map<String,String> params){
        System.out.println("异步回调....");
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);// 验签
        if(result==null || result.hasError()){
            return "fail";
        }
        boolean signVerified = result.getData(); //调用SDK验证签名
        if(signVerified){
            //验签成功
            String orderNo = params.get("out_trade_no");
            //修改订单的状态
            int effectCount = orderInfoService.changePayStatus(orderNo, OrderInfo.STATUS_ACCOUNT_PAID, OrderInfo.PAYTYPE_ONLINE);
            if(effectCount==0){
                //发消息通知客服,走退款的流程
                /***
                 * 场景1：重复回调
                 * 支付平台多次回调，第一次已成功处理，第二次 effectCount==0。
                 * 此时订单已是“已支付”，不应再自动退款。
                 * 场景2：订单已被取消
                 * 用户下单后超时未支付，订单被系统自动取消。
                 * 但支付平台延迟回调，effectCount==0。
                 * 此时直接退款，可能导致用户未实际支付却收到退款。
                 */
            }
        }
        return "success";
    }
    @Value("${pay.errorUrl}")
    private String errorUrl;
    @Value("${pay.frontEndPayUrl}")
    private String frontEndPayUrl;
    //同步回调
    @RequestMapping("/returnUrl")
    public void returnUrl(@RequestParam Map<String,String> params, HttpServletResponse response) throws IOException {
        System.out.println("同步回调....");
        Result<Boolean> result = payFeignApi.rsaCheckV1(params);// 验签
        if(result==null || result.hasError() || !result.getData()){
            response.sendRedirect(errorUrl);// 验签失败，跳转到错误页面
            return;
        }
        String orderNo = params.get("out_trade_no");
        response.sendRedirect(frontEndPayUrl+orderNo);// 跳转到前端支付结果页
    }
}
