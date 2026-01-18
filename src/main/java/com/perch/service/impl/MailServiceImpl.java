package com.perch.service.impl;

import com.perch.constants.RedisConstants;
import com.perch.exception.CustomException;
import com.perch.service.MailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static com.perch.constants.RedisConstants.VERIFY_CODE_PREFIX;

/**
 * @Author: lsh
 * @Date: 2025/12/11/16:57
 * @Description:
 */
@Service
@Slf4j
public class MailServiceImpl implements MailService {

    private final JavaMailSender mailSender;
    private final StringRedisTemplate redisTemplate; // Redis 工具

    // 邮件发件人
    @Value("${spring.mail.username}")
    private String fromEmail;

    public MailServiceImpl(JavaMailSender mailSender, StringRedisTemplate redisTemplate) {
        this.mailSender = mailSender;
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void sendVerificationCode(String toEmail) {
        // 1. 生成 6 位随机数字
        String code = String.valueOf((int)((Math.random() * 9 + 1) * 100000));

        String redisKey = VERIFY_CODE_PREFIX + toEmail;
        redisTemplate.opsForValue().set(redisKey, code, 5, TimeUnit.MINUTES);

        // 3. 发送邮件
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("【Perch栖息】您的注册验证码");
        message.setText("欢迎注册 Perch 心理咨询平台。\n您的验证码是：" + code + "\n有效期 5 分钟，请勿泄露给他人。");

        try {
            mailSender.send(message);
            log.info("邮件已发送至: {}, 验证码: {}", toEmail, code);
        } catch (Exception e) {
            log.error("邮件发送失败", e);
            throw new CustomException("邮件发送失败，请检查邮箱是否正确");
        }
    }
}
