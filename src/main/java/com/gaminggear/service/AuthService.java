package com.gaminggear.service;

import com.gaminggear.exception.BusinessException;
import com.gaminggear.exception.ResourceNotFoundException;
import com.gaminggear.model.User;
import com.gaminggear.model.enums.UserRole;
import com.gaminggear.repository.UserRepository;
import com.gaminggear.storage.StorageSync;
import com.gaminggear.util.IdGenerator;
import com.gaminggear.util.ValidationUtil;

import java.util.List;
import java.util.Optional;

public class AuthService {

    private final UserRepository userRepository;
    private final StorageSync storageSync;
    private User currentUser;

    public AuthService(UserRepository userRepository, StorageSync storageSync) {
        this.userRepository = userRepository;
        this.storageSync = storageSync;
    }

    public User login(String username, String password) {
        String cleanUser = ValidationUtil.requireNonBlank(username, "Tên đăng nhập");
        String cleanPass = ValidationUtil.requireNonBlank(password, "Mật khẩu");

        Optional<User> found = userRepository.findByUsername(cleanUser);
        if (found.isEmpty() || !found.get().getPassword().equals(cleanPass)) {
            throw new BusinessException("Tên đăng nhập hoặc mật khẩu không chính xác.");
        }
        User user = found.get();
        if (!user.isActive()) {
            throw new BusinessException("Tài khoản này đã bị vô hiệu hóa.");
        }
        this.currentUser = user;
        return user;
    }

    public void logout() {
        this.currentUser = null;
    }

    public Optional<User> getCurrentUser() {
        return Optional.ofNullable(currentUser);
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User createUser(String username, String password, String fullName, UserRole role) {
        String cleanUsername = ValidationUtil.requireNonBlank(username, "Tên đăng nhập");
        String cleanPassword = ValidationUtil.requireNonBlank(password, "Mật khẩu");
        String cleanFullName = ValidationUtil.requireNonBlank(fullName, "Họ tên nhân viên");
        if (cleanPassword.length() < 6) {
            throw new BusinessException("Mật khẩu phải có ít nhất 6 ký tự.");
        }
        if (userRepository.findByUsername(cleanUsername).isPresent()) {
            throw new BusinessException("Tên đăng nhập đã tồn tại: " + cleanUsername);
        }
        User user = new User(
                IdGenerator.nextId("USR"),
                cleanUsername,
                cleanPassword,
                cleanFullName,
                role == null ? UserRole.STAFF : role,
                true
        );
        userRepository.save(user);
        persist();
        return user;
    }

    public User updateUserStatus(String userId, boolean active, UserRole role, String fullName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + userId));
        if ("admin".equalsIgnoreCase(user.getUsername()) && !active) {
            throw new BusinessException("Không thể khóa tài khoản quản trị mặc định (admin).");
        }
        user.setFullName(ValidationUtil.requireNonBlank(fullName, "Họ tên nhân viên"));
        user.setRole(role == null ? user.getRole() : role);
        user.setActive(active);
        userRepository.save(user);
        persist();
        return user;
    }

    public void changePassword(String userId, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy tài khoản: " + userId));
        String cleanPassword = ValidationUtil.requireNonBlank(newPassword, "Mật khẩu mới");
        if (cleanPassword.length() < 6) {
            throw new BusinessException("Mật khẩu mới phải có ít nhất 6 ký tự.");
        }
        user.setPassword(cleanPassword);
        userRepository.save(user);
        persist();
    }

    private void persist() {
        if (storageSync != null) {
            storageSync.persistIfEnabled();
        }
    }
}
