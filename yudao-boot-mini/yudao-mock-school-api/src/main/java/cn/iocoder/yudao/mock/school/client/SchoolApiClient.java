package cn.iocoder.yudao.mock.school.client;

import cn.iocoder.yudao.mock.school.model.SchoolUserInfo;
import cn.iocoder.yudao.mock.school.exception.SchoolApiException;

/**
 * 学校API客户端接口
 * 支持Mock模式（内存数据）和Real模式（真实学校API调用）
 * 
 * @author Claude
 * @since 2025-09-04
 */
public interface SchoolApiClient {

    /**
     * 通过学校网关认证并获取用户信息
     *
     * @param username 学号/工号
     * @param password 密码
     * @return 学校侧用户信息
     * @throws SchoolApiException 封装的调用异常
     */
    SchoolUserInfo login(String username, String password) throws SchoolApiException;
    
    /**
     * 获取当前API客户端模式
     * 
     * @return API模式：MOCK 或 REAL
     */
    String getMode();
}