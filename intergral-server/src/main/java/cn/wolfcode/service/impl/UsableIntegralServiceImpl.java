package cn.wolfcode.service.impl;

import cn.wolfcode.common.exception.BusinessException;
import cn.wolfcode.domain.AccountTransaction;
import cn.wolfcode.domain.OperateIntergralVo;
import cn.wolfcode.mapper.AccountTransactionMapper;
import cn.wolfcode.mapper.UsableIntegralMapper;
import cn.wolfcode.service.IUsableIntegralService;
import cn.wolfcode.web.msg.IntergralCodeMsg;
import com.alibaba.fastjson.JSONObject;
import io.seata.rm.tcc.api.BusinessActionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * Created by lanxw
 */
@Service
public class UsableIntegralServiceImpl implements IUsableIntegralService {
    @Autowired
    private UsableIntegralMapper usableIntegralMapper;
    @Autowired
    private AccountTransactionMapper accountTransactionMapper;

    @Override
    @Transactional
    public void decrIntegral(OperateIntergralVo vo) {
        int effectCount = usableIntegralMapper.decrIntergral(vo.getUserId(),vo.getValue());
        if(effectCount==0){
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    public void incrIntegral(OperateIntergralVo vo) {
        usableIntegralMapper.incrIntergral(vo.getUserId(),vo.getValue());
    }

    @Override
    @Transactional
    public void decrIntegralTry(OperateIntergralVo vo, BusinessActionContext context) {
        System.out.println("执行TRY方法");
        //插入事务控制表
        AccountTransaction log = new AccountTransaction();
        log.setTxId(context.getXid());//全局事务ID
        log.setActionId(context.getBranchId());//分支事务ID
        Date now = new Date();
        log.setGmtCreated(now);
        log.setGmtModified(now);
        log.setUserId(vo.getUserId());
        log.setAmount(vo.getValue());
        accountTransactionMapper.insert(log);
        //执行业务逻辑--->减积分
        int effectCount = usableIntegralMapper.decrIntergral(vo.getUserId(), vo.getValue());
        if(effectCount==0){
            throw new BusinessException(IntergralCodeMsg.INTERGRAL_NOT_ENOUGH);
        }
    }

    @Override
    public void decrIntegralCommit(BusinessActionContext context) {
        System.out.println("执行COMMIT方法");
        JSONObject jsonObject = (JSONObject) context.getActionContext("vo");
        OperateIntergralVo vo = jsonObject.toJavaObject(OperateIntergralVo.class);
        System.out.println("vo对象:"+vo);
        //查询事务记录
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if(accountTransaction!=null){
            //如果不为空
            if(AccountTransaction.STATE_TRY==accountTransaction.getState()){
                //如果状态为TRY,执行COMMIT逻辑
                //更新日志的状态  空操作
                accountTransactionMapper.updateAccountTransactionState(context.getXid(), context.getBranchId(), AccountTransaction.STATE_COMMIT, AccountTransaction.STATE_TRY);
            }else if(AccountTransaction.STATE_COMMIT==accountTransaction.getState()){
                //如果状态为COMMIT,不做事情
            }else{
                //如果状态是其他--->写MQ通知管理员
            }
        }else{
            //如果为空--->写MQ通知管理员
        }
    }

    @Override
    @Transactional
    public void decrIntegralRollback(BusinessActionContext context) {
        System.out.println("执行ROLLBACK方法");
        AccountTransaction accountTransaction = accountTransactionMapper.get(context.getXid(), context.getBranchId());
        if(accountTransaction!=null){
            //存在日志记录
            if(AccountTransaction.STATE_TRY == accountTransaction.getState()){
                //处于TRY状态
                //将状态修改成Cancel状态
                accountTransactionMapper.updateAccountTransactionState(context.getXid(),context.getBranchId(),AccountTransaction.STATE_CANCEL,AccountTransaction.STATE_TRY);
                //执行Cancel业务逻辑,添加积分.
                usableIntegralMapper.incrIntergral(accountTransaction.getUserId(),accountTransaction.getAmount());
            }else if (AccountTransaction.STATE_CANCEL == accountTransaction.getState()){
                //之前已经执行过Cancel,幂等处理
            }else{
                //其他情况,通知管理员
            }
        }else{
          /*  String str = (String) context.getActionContext("vo");
            System.out.println("存储上下文的对象:"+str);
            OperateIntergralVo vo = JSON.parseObject(str,OperateIntergralVo.class);*/
            JSONObject jsonObject = (JSONObject) context.getActionContext("vo");
            OperateIntergralVo vo = jsonObject.toJavaObject(OperateIntergralVo.class);
            //不存在日志记录
            //插入事务控制表
            AccountTransaction log = new AccountTransaction();
            log.setTxId(context.getXid());//全局事务ID
            log.setActionId(context.getBranchId());//分支事务ID
            Date now = new Date();
            log.setGmtCreated(now);
            log.setGmtModified(now);
            log.setUserId(vo.getUserId());
            log.setAmount(vo.getValue());
            log.setState(AccountTransaction.STATE_CANCEL);
            accountTransactionMapper.insert(log);
        }
    }
}
