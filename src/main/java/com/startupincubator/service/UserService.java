package com.startupincubator.service;

import com.startupincubator.entity.User;
import java.util.List;
import java.util.Optional;

public interface UserService {
    User registerUser(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(Long id);
    List<User> findAllUsers();
    User saveUser(User user);
    void deleteUser(Long id);
    boolean existsByEmail(String email);
}