package com.montelzek.moneytrack.controller;

import com.montelzek.moneytrack.dto.UserRegisterDTO;
import com.montelzek.moneytrack.model.Role;
import com.montelzek.moneytrack.model.User;
import com.montelzek.moneytrack.repository.RoleRepository;
import com.montelzek.moneytrack.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;

@Controller
public class AuthController {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegistrationForm(Model model) {
        model.addAttribute("user", new UserRegisterDTO());
        return "register";
    }

    @PostMapping("/register")
    public String saveUser(@ModelAttribute("user") UserRegisterDTO userRegisterDTO, BindingResult result) {
        if (result.hasErrors()) {
            return "register";
        }

        if (userRepository.existsByEmail(userRegisterDTO.getEmail())) {
            result.rejectValue("email", "error.email", "Email already exists");
            return "register";
        }

        User user = new User(
                userRegisterDTO.getEmail(),
                passwordEncoder.encode(userRegisterDTO.getPassword()),
                userRegisterDTO.getFirstName(),
                userRegisterDTO.getLastName()
        );

        Role userRole = roleRepository.findByName(Role.ERole.ROLE_USER)
                .orElseThrow(() -> new RuntimeException("Role not found."));
        user.setRoles(Collections.singleton(userRole));

        userRepository.save(user);

        return "redirect:/login?registered=true";
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";
    }
}
