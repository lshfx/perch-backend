package com.perch.pojo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: lsh
 * @Date: 2025/12/11/17:33
 * @Description:
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginResponse {

    /**
     * JWT 令牌 (前端需存入 LocalStorage，后续请求放在 Header 里)
     */
    private String token;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 刷新令牌
     *
     */
    private String refreshToken;

    /**
     * 用户昵称 (用于右上角显示)
     */
    private String nickname;

    /**
     * 用户头像 (用于右上角显示)
     */
    private String avatarUrl;

    /**
     * 用户角色 (USER/ADMIN)
     * 前端根据这个字段决定是否显示“管理后台”入口
     */
    private String role;

    /**
     * 邮箱 (可选，个人中心回显用)
     */
    private String email;
}
