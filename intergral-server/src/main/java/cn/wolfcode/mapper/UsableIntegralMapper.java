package cn.wolfcode.mapper;

import org.apache.ibatis.annotations.Param;

/**
 * Created by lanxw
 */
public interface UsableIntegralMapper {
    /**
     * 冻结用户积分金额
     * @param userId
     * @param amount
     * @return
     */
    int freezeIntergral(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 提交改变，冻结金额真实扣除
     * @param userId
     * @param amount
     * @return
     */
    int commitChange(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 取消冻结金额
     * @param userId
     * @param amount
     */
    void unFreezeIntergral(@Param("userId") Long userId, @Param("amount") Long amount);

    /**
     * 增加积分
     * @param userId
     * @param amount
     */
    void incrIntergral(@Param("userId") Long userId, @Param("amount") Long amount);
    /**
     * 减少积分
     * @param userId
     * @param amount
     */
    int decrIntergral(@Param("userId") Long userId, @Param("amount") Long amount);
}
