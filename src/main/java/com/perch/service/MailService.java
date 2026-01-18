package com.perch.service;

/**
 * @Author: lsh
 * @Date: 2025/12/11/16:50
 * @Description:
 */
public interface MailService{
    /**
     * 发送邮箱验证码
     * @param toEmail 接收者邮箱信息
     * @return: void
     */
    void sendVerificationCode(String toEmail);
}
