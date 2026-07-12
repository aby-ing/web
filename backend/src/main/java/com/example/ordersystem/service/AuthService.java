package com.example.ordersystem.service;

import com.example.ordersystem.domain.Role;
import com.example.ordersystem.domain.User;
import com.example.ordersystem.dto.Dtos;
import com.example.ordersystem.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final PasswordService passwordService;
    private final Map<String, Long> tokenStore = new ConcurrentHashMap<>();

    public AuthService(UserRepository userRepository, PasswordService passwordService) {
        this.userRepository = userRepository;
        this.passwordService = passwordService;
    }

    @Transactional
    public Dtos.AuthResult register(Dtos.RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }

        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordService.hash(request.password()));
        user.setNickname(request.nickname().trim());
        user.setPhone(trimToEmpty(request.phone()));
        user.setRole(Role.USER);
        userRepository.save(user);
        return issueToken(user);
    }

    public Dtos.AuthResult login(Dtos.LoginRequest request) {
        String username = normalizeUsername(request.username());
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.isActive() || !passwordService.matches(request.password(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        if (user.getRole() == Role.ADMIN) {
            throw new SecurityException("管理员模块已停用");
        }
        return issueToken(user);
    }

    public void logout(String token) {
        if (StringUtils.hasText(token)) {
            tokenStore.remove(token);
        }
    }

    public User requireUser(String token) {
        if (!StringUtils.hasText(token)) {
            throw new SecurityException("请先登录");
        }
        Long userId = tokenStore.get(token);
        if (userId == null) {
            throw new SecurityException("登录已失效，请重新登录");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new SecurityException("用户不存在"));
        if (!user.isActive()) {
            throw new SecurityException("账号已停用");
        }
        return user;
    }

    public User requireAdmin(String token) {
        User user = requireUser(token);
        if (user.getRole() != Role.ADMIN) {
            throw new SecurityException("需要管理员权限");
        }
        return user;
    }

    public User requireMerchant(String token) {
        User user = requireUser(token);
        if (user.getRole() != Role.MERCHANT) {
            throw new SecurityException("需要商家权限");
        }
        return user;
    }

    public User requireMerchantOrAdmin(String token) {
        User user = requireUser(token);
        if (user.getRole() != Role.MERCHANT && user.getRole() != Role.ADMIN) {
            throw new SecurityException("需要商家或管理员权限");
        }
        return user;
    }

    public Dtos.UserView me(String token) {
        return toUserView(requireUser(token));
    }

    public Dtos.UserView toUserView(User user) {
        return new Dtos.UserView(
                user.getId(),
                user.getUsername(),
                user.getNickname(),
                user.getPhone(),
                user.getRole()
        );
    }

    private Dtos.AuthResult issueToken(User user) {
        String token = UUID.randomUUID().toString().replace("-", "");
        tokenStore.put(token, user.getId());
        return new Dtos.AuthResult(token, toUserView(user));
    }

    private String normalizeUsername(String username) {
        return username.trim().toLowerCase(Locale.ROOT);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}
