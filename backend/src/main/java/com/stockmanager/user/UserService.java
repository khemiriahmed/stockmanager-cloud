package com.stockmanager.user;

import com.stockmanager.common.dto.PageResponse;
import com.stockmanager.common.exception.BusinessException;
import com.stockmanager.role.Role;
import com.stockmanager.role.RoleName;
import com.stockmanager.role.RoleRepository;
import com.stockmanager.user.dto.ChangePasswordRequest;
import com.stockmanager.user.dto.CreateUserRequest;
import com.stockmanager.user.dto.UpdateProfileRequest;
import com.stockmanager.user.dto.UpdateRolesRequest;
import com.stockmanager.user.dto.UpdateUserRequest;
import com.stockmanager.user.dto.UserDto;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.Set;

@Service
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public PageResponse<UserDto> list(String search, Pageable pageable) {
        String query = StringUtils.hasText(search) ? search.trim() : "";
        return PageResponse.of(userRepository.search(query, pageable), UserDto::from);
    }

    @Transactional(readOnly = true)
    public UserDto getById(Long id) {
        return UserDto.from(findUser(id));
    }

    @Transactional(readOnly = true)
    public UserDto getByEmail(String email) {
        return UserDto.from(findByEmail(email));
    }

    public UserDto create(CreateUserRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("Un compte existe déjà avec cet email.");
        }
        Set<Role> roles = resolveRoles(request.roles());
        User user = new User(request.firstName().trim(), request.lastName().trim(), email,
                passwordEncoder.encode(request.password()), roles);
        return UserDto.from(userRepository.save(user));
    }

    public UserDto update(Long id, UpdateUserRequest request) {
        User user = findUser(id);
        String email = request.email().trim().toLowerCase();
        if (!user.getEmail().equalsIgnoreCase(email) && userRepository.existsByEmailIgnoreCase(email)) {
            throw BusinessException.conflict("Un compte existe déjà avec cet email.");
        }
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setEmail(email);
        return UserDto.from(user);
    }

    public UserDto updateStatus(Long id, boolean active, String currentEmail) {
        User user = findUser(id);
        if (!active && user.getEmail().equalsIgnoreCase(currentEmail)) {
            throw BusinessException.conflict("Vous ne pouvez pas désactiver votre propre compte.");
        }
        user.setActive(active);
        return UserDto.from(user);
    }

    public UserDto updateRoles(Long id, UpdateRolesRequest request, String currentEmail) {
        User user = findUser(id);
        Set<Role> roles = resolveRoles(request.roles());
        boolean removesOwnAdmin = user.getEmail().equalsIgnoreCase(currentEmail)
                && user.hasRole(RoleName.ADMIN)
                && request.roles().stream().noneMatch(role -> role == RoleName.ADMIN);
        if (removesOwnAdmin) {
            throw BusinessException.conflict("Vous ne pouvez pas retirer votre propre rôle ADMIN.");
        }
        user.setRoles(roles);
        return UserDto.from(user);
    }

    public UserDto updateProfile(String email, UpdateProfileRequest request) {
        User user = findByEmail(email);
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        return UserDto.from(user);
    }

    public void changePassword(String email, ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BusinessException("Le mot de passe actuel est incorrect.", "INVALID_PASSWORD",
                    org.springframework.http.HttpStatus.BAD_REQUEST);
        }
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }

    public User findByEmail(String email) {
        return userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> BusinessException.notFound("Utilisateur introuvable."));
    }

    private User findUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> BusinessException.notFound("Utilisateur introuvable."));
    }

    private Set<Role> resolveRoles(Set<RoleName> roleNames) {
        Set<Role> roles = new HashSet<>();
        for (RoleName roleName : roleNames) {
            roles.add(roleRepository.findByName(roleName)
                    .orElseThrow(() -> BusinessException.notFound("Rôle inconnu: " + roleName)));
        }
        return roles;
    }
}
