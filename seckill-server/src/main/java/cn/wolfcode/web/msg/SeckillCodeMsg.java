package cn.wolfcode.web.msg;
import cn.wolfcode.common.web.CodeMsg;

/**
 * Created by wolfcode-lanxw
 */
public class SeckillCodeMsg extends CodeMsg {
    private SeckillCodeMsg(Integer code, String msg){
        super(code,msg);
    }
    public static final SeckillCodeMsg SECKILL_STOCK_OVER = new SeckillCodeMsg(500201,"您来晚了，商品已经被抢购完毕.");
    public static final SeckillCodeMsg REPEAT_SECKILL = new SeckillCodeMsg(500202,"您已经抢购到商品了，请不要重复抢购");
    public static final SeckillCodeMsg SECKILL_ERROR = new SeckillCodeMsg(500203,"秒杀失败");
    public static final SeckillCodeMsg CANCEL_ORDER_ERROR = new SeckillCodeMsg(500204,"超时取消失败");
    public static final SeckillCodeMsg PAY_SERVER_ERROR = new SeckillCodeMsg(500205,"支付服务繁忙，稍后再试");
    public static final SeckillCodeMsg REFUND_ERROR = new SeckillCodeMsg(500206,"退款失败，请联系管理员");
    public static final SeckillCodeMsg INTERGRAL_SERVER_ERROR = new SeckillCodeMsg(500207,"操作积分失败");
    public static final SeckillCodeMsg PRODUCT_SERVER_ERROR = new SeckillCodeMsg(500208,"商品服务繁忙，稍后再试.");
    public static final SeckillCodeMsg PAY_ERROR = new SeckillCodeMsg(500209,"支付失败");
    public static final SeckillCodeMsg PAY_STATUS_CHANGE = new SeckillCodeMsg(500210,"支付状态发送改变,刷新页面");
    public static final SeckillCodeMsg PRODUCT_NOT_EXIST = new SeckillCodeMsg(500211,"商品不存在");
}
