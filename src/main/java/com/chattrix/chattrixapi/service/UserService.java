package com.chattrix.chattrixapi.service;

import com.chattrix.chattrixapi.model.User;
import com.chattrix.chattrixapi.repository.UserRepository;
import com.chattrix.chattrixapi.request.UserCreateRequest;
import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.CursoredPage;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.Optional;

@ApplicationScoped
public class UserService {

    @Inject
    UserRepository userRepository;

    public Page<User> getAll(int page, int size, String sortField, boolean asc) {
        PageRequest pr = PageRequest.ofPage(page + 1, size, true);
        Order<User> order = asc
                ? Order.by(Sort.asc(sortField))
                : Order.by(Sort.desc(sortField));
        return userRepository.findAll(pr, order);
    }

    public Page<User> search(String keyword, int page, int size, String sortField, boolean asc) {
        PageRequest pr = PageRequest.ofPage(page + 1, size, true);
        Order<User> order = asc
                ? Order.by(Sort.asc(sortField))
                : Order.by(Sort.desc(sortField));
        return userRepository.search(keyword, pr, order);
    }

    public Page<User> filterByStatus(String status, int page, int size, String sortField, boolean asc) {
        PageRequest pr = PageRequest.ofPage(page + 1, size, true);
        Order<User> order = asc
                ? Order.by(Sort.asc(sortField))
                : Order.by(Sort.desc(sortField));
        return userRepository.findByStatus(status, pr, order);
    }

    public CursoredPage<User> filterByStatusCursor(String status, PageRequest pr, Order<User> order, Limit limit) {
        return userRepository.findByStatus(status, pr, order, limit);
    }

    public Optional<User> getUserById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User create(UserCreateRequest req) {
        User u = User.builder()
                .username(req.getUsername())
                .password(hash(req.getPassword()))
                .email(req.getEmail())
                .fullName(req.getFullName())
                .avatarUrl(req.getAvatarUrl())
                .status("online")
                .build();
        return userRepository.save(u);
    }

    @Transactional
    public User update(User user) {
        return userRepository.save(user);
    }

    @Transactional
    public void delete(Long id) {
        userRepository.deleteById(id);
    }

    private String hash(String password) {
        // TODO: Replace with BCrypt/Argon2
        return "{bcrypt}" + password;
    }
}
