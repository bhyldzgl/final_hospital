package com.hospital.automation.service;

import com.hospital.automation.common.exception.BadRequestException;
import com.hospital.automation.config.security.JwtTokenProvider;
import com.hospital.automation.domain.dto.request.LoginRequest;
import com.hospital.automation.domain.dto.request.RegisterRequest;
import com.hospital.automation.domain.dto.response.AuthResponse;
import com.hospital.automation.domain.entity.Role;
import com.hospital.automation.domain.entity.User;
import com.hospital.automation.repository.RoleRepository;
import com.hospital.automation.repository.UserRepository;
import com.hospital.automation.service.impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtTokenProvider tokenProvider;

    @InjectMocks
    private AuthServiceImpl authService;

    private Role rolePatient;
    private Role roleAdmin;

    @BeforeEach
    void setup() {
        rolePatient = Role.builder().id(1L).name("ROLE_PATIENT").build();
        roleAdmin   = Role.builder().id(2L).name("ROLE_ADMIN").build();
    }

    // -------------------------------------------------------------------------
    // REGISTER TESTLERİ
    // -------------------------------------------------------------------------

    @Test
    void register_shouldThrowBadRequest_whenUsernameAlreadyExists() {
        // Arrange (Hazırlık)
        RegisterRequest request = new RegisterRequest(
                "berfin",
                "berfin@test.com",
                "123456",
                Set.of("PATIENT")
        );

        when(userRepository.existsByUsername("berfin")).thenReturn(true);

        // Act + Assert (Çalıştır + Doğrula)
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("username already exists", ex.getMessage());

        // Bu durumda devam etmemeli:
        verify(userRepository, never()).existsByEmail(anyString());
        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenProvider, never()).generateToken(anyString(), anyList());
    }

    @Test
    void register_shouldThrowBadRequest_whenEmailAlreadyExists() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "berfin",
                "berfin@test.com",
                "123456",
                Set.of("PATIENT")
        );

        when(userRepository.existsByUsername("berfin")).thenReturn(false);
        when(userRepository.existsByEmail("berfin@test.com")).thenReturn(true);

        // Act + Assert
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("email already exists", ex.getMessage());

        verify(roleRepository, never()).findByName(anyString());
        verify(userRepository, never()).save(any(User.class));
        verify(tokenProvider, never()).generateToken(anyString(), anyList());
    }

    @Test
    void register_shouldDefaultToRolePatient_whenRolesNull() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "berfin",
                "berfin@test.com",
                "123456",
                null // roles null -> normalizeRoles => ROLE_PATIENT
        );

        when(userRepository.existsByUsername("berfin")).thenReturn(false);
        when(userRepository.existsByEmail("berfin@test.com")).thenReturn(false);

        when(roleRepository.findByName("ROLE_PATIENT")).thenReturn(Optional.of(rolePatient));
        when(passwordEncoder.encode("123456")).thenReturn("ENC(123456)");

        // save() dönecek user (id set edelim)
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0, User.class);
            u.setId(10L);
            return u;
        });

        when(tokenProvider.generateToken(eq("berfin"), anyList())).thenReturn("jwt-token");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertNotNull(response);
        assertEquals("jwt-token", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(10L, response.userId());
        assertEquals("berfin", response.username());
        assertEquals(Set.of("ROLE_PATIENT"), response.roles());

        // Kayıtta gerçekten ROLE_PATIENT istenmiş mi?
        verify(roleRepository).findByName("ROLE_PATIENT");

        // Password encode edilmiş mi?
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User savedArg = captor.getValue();
        assertEquals("ENC(123456)", savedArg.getPasswordHash());
        assertEquals(Set.of("ROLE_PATIENT"), savedArg.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet()));

        // Token üretimi doğru mu?
        verify(tokenProvider).generateToken(eq("berfin"), eq(List.of("ROLE_PATIENT")));
    }

    @Test
    void register_shouldNormalizeRoles_whenRolePrefixMissing() {
        // Arrange: "admin" -> "ROLE_ADMIN"
        RegisterRequest request = new RegisterRequest(
                "berfin",
                "berfin@test.com",
                "123456",
                Set.of("admin") // normalize => ROLE_ADMIN
        );

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(passwordEncoder.encode(anyString())).thenReturn("ENC");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0, User.class);
            u.setId(11L);
            return u;
        });

        when(tokenProvider.generateToken(eq("berfin"), eq(List.of("ROLE_ADMIN")))).thenReturn("jwt-admin");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertEquals(11L, response.userId());
        assertEquals(Set.of("ROLE_ADMIN"), response.roles());
        assertEquals("jwt-admin", response.token());

        verify(roleRepository).findByName("ROLE_ADMIN");
        verify(tokenProvider).generateToken(eq("berfin"), eq(List.of("ROLE_ADMIN")));
    }

    @Test
    void register_shouldThrowBadRequest_whenRoleNotFound() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "berfin",
                "berfin@test.com",
                "123456",
                Set.of("ROLE_ADMIN") // direkt veriyoruz ama DB'de yok varsayalım
        );

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.empty());

        // Act + Assert
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.register(request)
        );

        assertEquals("Role not found: ROLE_ADMIN", ex.getMessage());

        // Role bulunamadığı için user save ve token üretimi olmamalı
        verify(userRepository, never()).save(any(User.class));
        verify(tokenProvider, never()).generateToken(anyString(), anyList());
    }

    @Test
    void register_shouldReturnAuthResponse_whenSuccess_multipleRoles() {
        // Arrange
        RegisterRequest request = new RegisterRequest(
                "berfin",
                "berfin@test.com",
                "123456",
                Set.of("ADMIN", "PATIENT") // normalize => ROLE_ADMIN, ROLE_PATIENT
        );

        when(userRepository.existsByUsername("berfin")).thenReturn(false);
        when(userRepository.existsByEmail("berfin@test.com")).thenReturn(false);

        // İki rol DB'den bulunacak
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(roleAdmin));
        when(roleRepository.findByName("ROLE_PATIENT")).thenReturn(Optional.of(rolePatient));

        when(passwordEncoder.encode("123456")).thenReturn("ENC(123456)");

        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0, User.class);
            u.setId(12L);
            return u;
        });

        // tokenProvider'a hangi rol listesi gittiğini anyList() ile geçeceğiz,
        // sonra verify ile kontrol edeceğiz (sıra set'ten geldiği için değişebilir)
        when(tokenProvider.generateToken(eq("berfin"), anyList())).thenReturn("jwt-multi");

        // Act
        AuthResponse response = authService.register(request);

        // Assert
        assertEquals(12L, response.userId());
        assertEquals("berfin", response.username());
        assertEquals("jwt-multi", response.token());
        assertEquals("Bearer", response.tokenType());

        // roles set olarak gelmeli
        assertTrue(response.roles().contains("ROLE_ADMIN"));
        assertTrue(response.roles().contains("ROLE_PATIENT"));
        assertEquals(2, response.roles().size());

        // Save edilen user doğru mu?
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertEquals("ENC(123456)", saved.getPasswordHash());

        Set<String> savedRoleNames = saved.getRoles().stream().map(Role::getName).collect(java.util.stream.Collectors.toSet());
        assertEquals(Set.of("ROLE_ADMIN", "ROLE_PATIENT"), savedRoleNames);

        // token üretimi username ile çağrıldı mı?
        verify(tokenProvider).generateToken(eq("berfin"), argThat(list ->
                list.contains("ROLE_ADMIN") && list.contains("ROLE_PATIENT") && list.size() == 2
        ));
    }

    // -------------------------------------------------------------------------
    // LOGIN TESTLERİ
    // -------------------------------------------------------------------------

    @Test
    void login_shouldReturnAuthResponse_whenCredentialsValid() {
        // Arrange
        LoginRequest request = new LoginRequest("berfin", "123456");

        // authenticate başarılı gibi davranacak
        Authentication auth = mock(Authentication.class);
        when(authenticationManager.authenticate(any())).thenReturn(auth);

        User user = User.builder()
                .id(20L)
                .username("berfin")
                .passwordHash("ENC")
                .roles(Set.of(roleAdmin, rolePatient))
                .build();

        when(userRepository.findByUsername("berfin")).thenReturn(Optional.of(user));
        when(tokenProvider.generateToken(eq("berfin"), anyList())).thenReturn("jwt-login");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertEquals("jwt-login", response.token());
        assertEquals("Bearer", response.tokenType());
        assertEquals(20L, response.userId());
        assertEquals("berfin", response.username());
        assertTrue(response.roles().contains("ROLE_ADMIN"));
        assertTrue(response.roles().contains("ROLE_PATIENT"));

        // authenticate çağrıldı mı?
        verify(authenticationManager).authenticate(any());

        // token doğru username ile üretildi mi?
        verify(tokenProvider).generateToken(eq("berfin"), argThat(list ->
                list.contains("ROLE_ADMIN") && list.contains("ROLE_PATIENT") && list.size() == 2
        ));
    }

    @Test
    void login_shouldThrowBadRequest_whenUserNotFoundEvenIfAuthSuccess() {
        // Arrange
        LoginRequest request = new LoginRequest("berfin", "123456");

        when(authenticationManager.authenticate(any())).thenReturn(mock(Authentication.class));
        when(userRepository.findByUsername("berfin")).thenReturn(Optional.empty());

        // Act + Assert
        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.login(request)
        );

        assertEquals("Invalid credentials", ex.getMessage());

        // Token üretimi olmamalı
        verify(tokenProvider, never()).generateToken(anyString(), anyList());
    }
}
