package com.perch.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.perch.constants.RedisConstants;
import com.perch.exception.CustomException;
import com.perch.mapper.UserMapper;
import com.perch.pojo.common.Result;
import com.perch.pojo.dto.request.RegisterRequest;
import com.perch.pojo.dto.response.LoginResponse;
import com.perch.pojo.entity.User;
import com.perch.service.TokenService;
import com.perch.service.UserService;
import com.perch.utils.WechatUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.*;

import static com.perch.constants.RedisConstants.VERIFY_CODE_PREFIX;
import static java.util.UUID.*;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private final UserMapper userMapper;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    private final RedisTemplate<String,Object> redisTemplate;

    private final WechatUtils wechatUtils;

    @Override
    @Transactional
    public LoginResponse register(RegisterRequest request) {
        // 1. 校验邮箱信息和微信id哪个存在
        boolean isEmailRegister = !StringUtils.isBlank(request.getEmail());
        boolean isWechatRegister = !StringUtils.isBlank(request.getWechatCode());

        //2. 校验是否有邮箱或者是微信信息，如果没有直接抛出异常信息
        if (!isEmailRegister && !isWechatRegister) {
            throw new CustomException(400,"注册失败：必须提供邮箱或微信账号信息");
        }

        // 3.1 邮箱注册，检查邮箱信息
        if (isEmailRegister) {
            // 3.1 必填项检查 (手动补上 DTO 里去掉的 @NotBlank)
            if (StringUtils.isBlank(request.getPassword())) {
                throw new CustomException(400, "邮箱注册必须设置密码");
            }
            if (StringUtils.isBlank(request.getEmailVerifyCode())) {
                throw new CustomException(400, "验证码不能为空");
            }

            // 3.2 校验验证码 (从 Redis 取)
            String redisKey = VERIFY_CODE_PREFIX + request.getEmail();
            String cacheCode = (String) redisTemplate.opsForValue().get(redisKey);
            if (StringUtils.isBlank(cacheCode) || !cacheCode.equals(request.getEmailVerifyCode())) {
                throw new CustomException(400, "验证码错误或已过期");
            }
            // 验证通过后删除验证码
            redisTemplate.delete(redisKey);

            // 3.3 检查邮箱是否已存在
            if (existsByEmail(request.getEmail())) {
                throw new CustomException(400, "该邮箱已被注册，请直接登录");
            }
        }

        Map<String,String> userInfoMap = null;
        // 4. 微信注册，检查微信openid信息
        if (isWechatRegister) {
            // 4.1 检查微信是否已存在
            // (注意：如果已存在，这里其实应该直接返回登录成功，但这是注册接口，先抛错或直接返回旧用户)
            // 为了简单，如果是微信一键注册，我们先查一下，有就直接返回，没有就新建
            userInfoMap = wechatUtils.getOpenId(request.getWechatCode());
            User existingUser = userMapper.selectOne(new LambdaQueryWrapper<User>()
                    .eq(User::getWechatOpenid, userInfoMap.get("openid")));
            if (existingUser != null) {
                // 如果用户已存在，不能直接 return User，必须生成 Token 并返回 LoginResponse
                log.info("微信老用户直接登录: id={}", existingUser.getId());
                return processLogin(existingUser, request.getDeviceInfo());
            }
        }

        User user = new User();
        BeanUtil.copyProperties(request, user, "password", "wechatCode", "emailVerifyCode");

        if (userInfoMap != null) {
            user.setWechatOpenid(userInfoMap.get("openid"));
            // map.get("unionid") 即使返回 null 也没事，setter 允许 null
            user.setWechatUnionid(userInfoMap.get("unionid"));
        }

        // 处理密码
        if (StringUtils.isNotBlank(request.getPassword())) {
            user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        }

        // 设置默认值
        user.setStatus(1);
        user.setRole("USER");
        if (StringUtils.isBlank(user.getNickname())) {
            String prefix = isEmailRegister ? "用户_" : "微信用户_";
            user.setNickname(generateDefaultUsername(prefix, 6)); // 之前写的随机昵称方法
        }

        userMapper.insert(user);
        log.info("注册成功: id={}", user.getId());

        return processLogin(user,request.getDeviceInfo());
    }

    /**
     * 提取公共方法：负责生成 Token 并组装 LoginResponse
     * 避免在“新用户注册”和“老用户登录”两个地方重复写
     */
    private LoginResponse processLogin(User user, String deviceInfo) {
        // Use a non-empty subject for JWT: email -> openid -> userId.
        //降级处理，防止微信用户登录时邮箱为空造成程序崩溃
        String subject = user.getEmail();
        if (StringUtils.isBlank(subject)) {
            subject = user.getWechatOpenid();
        }
        if (StringUtils.isBlank(subject) && user.getId() != null) {
            subject = user.getId().toString();
        }

        // 调用 TokenService 生成双层 Redis 令牌
        Map<String, Object> tokenMap = tokenService.createToken(
                user.getId(),
                subject, // 建议用 Email 或 OpenID 作为 username 标识
                deviceInfo
        );

        // 组装返回
        return LoginResponse.builder()
                .token((String) tokenMap.get("token"))
                .refreshToken((String) tokenMap.get("refreshToken"))
                .userId(user.getId())
                .nickname(user.getNickname())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .email(user.getEmail())
                .build();
    }

    /*创建随机的默认用户名*/
    private String generateDefaultUsername(String prefix,int length) {
        // 生成UUID4（随机UUID），去掉横线后转小写
        String uuid = randomUUID().toString().replace("-", "").toLowerCase();
        if (length > 32) length = 32;
        if (length < 4) length = 4;
        // 拼接前缀，生成最终用户名
        return prefix + uuid.substring(uuid.length() - length);
    }

    @Override
    /*
     * 根据用户名查找用户
     */
    public Optional<User> findByUsername(String username) {
        // Accept multiple identifiers to match JWT subject (email/openid/userId).
        if (StringUtils.isBlank(username)) {
            return Optional.empty();
        }

        // 1) 邮箱
        Optional<User> byEmail = findByEmail(username);
        if (byEmail.isPresent()) {
            return byEmail;
        }

        // 2) 微信 openid
        User byOpenid = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getWechatOpenid, username));
        if (byOpenid != null) {
            return Optional.of(byOpenid);
        }

        // 3) userId
        if (username.chars().allMatch(Character::isDigit)) {
            return findById(Long.valueOf(username));
        }

        return Optional.empty();
    }

    @Override
    public Optional<User> findByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);

        User user = userMapper.selectOne(wrapper);
        return Optional.ofNullable(user);
    }

    @Override
    public boolean existsByUsername(String username) {
        // 由于新表使用邮箱作为登录标识，这里检查邮箱是否存在
        return existsByEmail(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("email", email);

        return userMapper.selectCount(wrapper) > 0;
    }

    @Override
    public Optional<User> findById(Long id) {
        User user = userMapper.selectById(id);
        return Optional.ofNullable(user);
    }

    @Override
    @Transactional
    public User update(User user) {
        userMapper.updateById(user);
        return user;
    }

    @Override
    @Transactional
    public boolean changePassword(Long userId, String oldPassword, String newPassword) {
        try {
            Optional<User> userOpt = findById(userId);
            if (userOpt.isEmpty()) {
                throw new RuntimeException("用户不存在");
            }

            User user = userOpt.get();

            // 验证旧密码
            if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
                return false;
            }

            // 更新新密码
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);

            return true;

        } catch (Exception e) {
            log.error("修改密码失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean resetPassword(String email, String newPassword) {
        try {
            Optional<User> userOpt = findByEmail(email);
            if (userOpt.isEmpty()) {
                return false;
            }

            User user = userOpt.get();
            user.setPasswordHash(passwordEncoder.encode(newPassword));
            userMapper.updateById(user);

            return true;

        } catch (Exception e) {
            log.error("重置密码失败: {}", e.getMessage());
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteById(Long id) {
        return userMapper.deleteById(id) > 0;
    }

    @Override
    public List<User> findAll() {
        // 新表没有逻辑删除字段，直接查询所有用户
        return userMapper.selectList(null);
    }


    @Override
    public User loadUserByUsername(String username) {
        Optional<User> userOpt =



                findByUsername(username);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("用户不存在: " + username);
        }
        return userOpt.get();
    }



    @Override
    public Map<String, Object> loginUser(String email, String password, String deviceInfo) {
        // 1. 参数验证
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("邮箱不能为空");
        }
        if (password == null || password.trim().isEmpty()) {
            throw new RuntimeException("密码不能为空");
        }

        // 2. 用户身份验证
        User user = authenticate(email, password);
        if (user == null) {
            throw new RuntimeException("邮箱或密码错误");
        }

        // 3. 创建Token
        Map<String, Object> tokenInfo = tokenService.createToken(
                user.getId(),
                user.getEmail(),
                deviceInfo != null ? deviceInfo : "未知设备"
        );

        // 4. 组装返回结果
        Map<String, Object> result = new HashMap<>(tokenInfo);
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("id", user.getId());
        userInfo.put("email", user.getEmail());
        userInfo.put("nickname", user.getNickname());
        userInfo.put("avatarUrl", user.getAvatarUrl());
        userInfo.put("role", user.getRole());
        result.put("user", userInfo);

        log.info("用户 {} 登录成功", email);
        return result;
    }

    @Override
    public Result<LoginResponse> loginByWechat(String wechatCode) {
        if (StringUtils.isBlank(wechatCode)) {
            throw new CustomException(400, "微信登录凭证不能为空");
        }

        Map<String, String> userInfoMap = wechatUtils.getOpenId(wechatCode);
        String openid = userInfoMap.get("openid");
        if (StringUtils.isBlank(openid)) {
            throw new CustomException(400, "获取用户openid失败");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                .eq(User::getWechatOpenid, openid));

        if (user == null) {
            user = new User();
            user.setWechatOpenid(openid);
            user.setWechatUnionid(userInfoMap.get("unionid"));
            user.setStatus(1);
            user.setRole("USER");
            user.setNickname(generateDefaultUsername("微信用户_", 6));
            userMapper.insert(user);
        } else if (user.getStatus() != 1) {
            throw new CustomException(403, "用户已被禁用");
        }

        return Result.success(processLogin(user, null));
    }

    @Override
    public Result<LoginResponse> loginByEmail(String email, String password) {
        if (StringUtils.isBlank(email) || StringUtils.isBlank(password)) {
            throw new CustomException(400, "邮箱或密码不能为空");
        }

        User user = authenticate(email, password);
        if (user == null) {
            throw new CustomException(401, "邮箱或密码错误");
        }

        return Result.success(processLogin(user, null));
    }

    @Override
    public Map<String, Object> getCurrentUserInfo() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new CustomException(401, "未登录");
        }

        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("username", authentication.getName());
        userInfo.put("authorities", authentication.getAuthorities());
        return userInfo;
    }


//    public User wechatUserAuth(String openid){
//        LambdaQueryWrapper<User> userLambdaQueryWrapper = new LambdaQueryWrapper<>();
//        userLambdaQueryWrapper.eq(User::getWechatOpenid,openid);
//    }

    public User authenticate(String username, String password) {
        try {
            // 1. 根据用户名查找用户
            Optional<User> userOpt = findByUsername(username);
            if (userOpt.isEmpty()) {
                log.warn("登录失败: 用户不存在 - {}", username);
                return null;
            }

            User user = userOpt.get();

            // 2. 检查用户状态
            if (user.getStatus() != 1) {
                log.warn("登录失败: 用户已被禁用 - {}", username);
                return null;
            }

            // 3. 验证密码
            if (!passwordEncoder.matches(password, user.getPasswordHash())) {
                log.warn("登录失败: 密码错误 - {}", username);
                return null;
            }

            // 4. 登录成功
            log.info("用户 {} 登录成功", username);
            return user;

        } catch (Exception e) {
            log.error("用户登录验证失败: {}", e.getMessage());
            return null;
        }
    }
}
