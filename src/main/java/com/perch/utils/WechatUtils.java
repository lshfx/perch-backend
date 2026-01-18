package com.perch.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WechatUtils {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    /**
     * 用 code 换取 openid
     * @return openid (如果失败则抛出异常)
     */
    public Map<String,String> getOpenId(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session" +
                "?appid=" + appid +
                "&secret=" + secret +
                "&js_code=" + code +
                "&grant_type=authorization_code";

        try {
            String response = restTemplate.getForObject(url, String.class);
            JsonNode jsonNode = objectMapper.readTree(response);

            // 检查是否有错误码
            if (jsonNode.has("errcode") && jsonNode.get("errcode").asInt() != 0) {
                log.error("微信登录失败: {}", response);
                throw new RuntimeException("微信登录失败: " + jsonNode.get("errmsg").asText());
            }

            Map<String, String> userInfoMap = new HashMap<>();
            userInfoMap.put("openid",jsonNode.get("openid").asText());
            if (jsonNode.has("unionid")) {
                userInfoMap.put("unionid", jsonNode.get("unionid").asText());
            }
            // 返回 openid
            return userInfoMap;
        } catch (Exception e) {
            log.error("调用微信接口异常", e);
            throw new RuntimeException("微信服务暂时不可用");
        }
    }
}