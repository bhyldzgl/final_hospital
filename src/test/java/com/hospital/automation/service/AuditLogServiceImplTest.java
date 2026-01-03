package com.hospital.automation.service;

import com.hospital.automation.config.security.UserPrincipal;
import com.hospital.automation.domain.dto.response.AuditLogResponse;
import com.hospital.automation.domain.entity.AuditLog;
import com.hospital.automation.repository.AuditLogRepository;
import com.hospital.automation.service.impl.AuditLogServiceImpl;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit Test = sadece Service mantığını test eder.
 * - DB yok
 * - Spring context yok
 * - Repository mock
 */
@ExtendWith(MockitoExtension.class)
class AuditLogServiceImplTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AuditLogServiceImpl auditLogService;

    @AfterEach
    void tearDown() {
        // Her testten sonra SecurityContext temizlenmezse diğer testleri etkileyebilir.
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // 1) log(...) TESTLERİ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("log(): Authentication varsa auth.getName() username olarak kaydedilir ve repository.save çağrılır")
    void log_shouldSaveAuditLog_withUsernameFromAuthName() {
        // Arrange (Hazırlık)
        // 1) SecurityContext'e bir Authentication koyuyoruz.
        var auth = new UsernamePasswordAuthenticationToken("berfin", "N/A");
        SecurityContextHolder.getContext().setAuthentication(auth);

        String action = "CREATE";
        String entityName = "Appointment";
        Long entityId = 100L;
        String details = "Appointment created (patientId=1, doctorId=2)";

        // createdAt için zaman aralığı yakalamak adına "önce" anını alıyoruz.
        LocalDateTime beforeCall = LocalDateTime.now().minusSeconds(1);

        // Act (Çalıştır)
        auditLogService.log(action, entityName, entityId, details);

        // Assert (Doğrula)
        // save içine giden AuditLog nesnesini yakalayıp alanlarını kontrol edeceğiz.
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository, times(1)).save(captor.capture());

        AuditLog saved = captor.getValue();

        assertEquals("berfin", saved.getUsername());
        assertEquals(action, saved.getAction());
        assertEquals(entityName, saved.getEntityName());
        assertEquals(entityId, saved.getEntityId());
        assertEquals(details, saved.getDetails());

        assertNotNull(saved.getCreatedAt());
        // createdAt "şimdi" set ediliyor mu? (çok kesin saniye beklemiyoruz, aralık kontrolü yapıyoruz)
        assertTrue(saved.getCreatedAt().isAfter(beforeCall),
                "createdAt, log çağrısından sonra set edilmiş olmalı");

        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("log(): Authentication yoksa username SYSTEM olarak kaydedilir")
    void log_shouldSaveAuditLog_withSystemUsername_whenNoAuthentication() {
        // Arrange
        // SecurityContext boş kalsın (auth = null)
        SecurityContextHolder.clearContext();

        // Act
        auditLogService.log("DELETE", "Department", 5L, "Department deleted");

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());

        AuditLog saved = captor.getValue();
        assertEquals("SYSTEM", saved.getUsername());
        assertEquals("DELETE", saved.getAction());
        assertEquals("Department", saved.getEntityName());
        assertEquals(5L, saved.getEntityId());
        assertEquals("Department deleted", saved.getDetails());
        assertNotNull(saved.getCreatedAt());

        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("log(): Principal UserPrincipal ise up.getUsername() kullanılır")
    void log_shouldUseUserPrincipalUsername_whenPrincipalIsUserPrincipal() {
        // Arrange
        // UserPrincipal'ı mock'luyoruz. (Class projenizde olduğu için derlenecek.)
        UserPrincipal up = mock(UserPrincipal.class);
        when(up.getUsername()).thenReturn("taha");

        var auth = new UsernamePasswordAuthenticationToken(up, "N/A");
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        auditLogService.log("UPDATE", "Patient", 77L, "Patient updated");

        // Assert
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertEquals("taha", saved.getUsername());
        assertEquals("UPDATE", saved.getAction());
        assertEquals("Patient", saved.getEntityName());
        assertEquals(77L, saved.getEntityId());
        assertEquals("Patient updated", saved.getDetails());

        verifyNoMoreInteractions(auditLogRepository);
    }

    // -------------------------------------------------------------------------
    // 2) getAll() TESTLERİ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("getAll(): repository.findAll() ne döndürürse onu döndürür")
    void getAll_shouldReturnAllAuditLogs() {
        // Arrange
        AuditLog a1 = AuditLog.builder().id(1L).username("u1").action("CREATE")
                .entityName("X").entityId(10L).createdAt(LocalDateTime.now()).details("d1").build();
        AuditLog a2 = AuditLog.builder().id(2L).username("u2").action("DELETE")
                .entityName("Y").entityId(20L).createdAt(LocalDateTime.now()).details("d2").build();

        when(auditLogRepository.findAll()).thenReturn(List.of(a1, a2));

        // Act
        List<AuditLog> result = auditLogService.getAll();

        // Assert
        assertEquals(2, result.size());
        assertEquals(1L, result.get(0).getId());
        assertEquals(2L, result.get(1).getId());

        verify(auditLogRepository, times(1)).findAll();
        verifyNoMoreInteractions(auditLogRepository);
    }

    // -------------------------------------------------------------------------
    // 3) search(...) TESTLERİ
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("search(): sort boşsa default createdAt desc ile findAll(spec, pageable) çağrılır ve response map edilir")
    void search_shouldDefaultSortCreatedAtDesc_andMapToResponse() {
        // Arrange
        AuditLog log = AuditLog.builder()
                .id(10L)
                .username("berfin")
                .action("CREATE")
                .entityName("Appointment")
                .entityId(99L)
                .createdAt(LocalDateTime.of(2026, 1, 1, 10, 0))
                .details("created")
                .build();

        // Repository findAll(spec, pageable) döndürsün:
        Page<AuditLog> pageFromRepo = new PageImpl<>(List.of(log));
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pageFromRepo);

        // Act
        Page<AuditLogResponse> result = auditLogService.search(
                "ber",          // username contains
                "CREATE",       // action equals
                "Appointment",  // entityName equals
                null,           // from
                null,           // to
                0,              // page
                5,              // size
                ""              // sort (blank) => default createdAt desc
        );

        // Assert: çıktı map edilmiş mi?
        assertEquals(1, result.getTotalElements());
        AuditLogResponse r = result.getContent().get(0);

        assertEquals(10L, r.id());
        assertEquals("berfin", r.username());
        assertEquals("CREATE", r.action());
        assertEquals("Appointment", r.entityName());
        assertEquals(99L, r.entityId());
        assertEquals(LocalDateTime.of(2026, 1, 1, 10, 0), r.createdAt());
        assertEquals("created", r.details());

        // Assert: repository doğru pageable ile çağrıldı mı?
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository, times(1)).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertEquals(0, used.getPageNumber());
        assertEquals(5, used.getPageSize());

        // default sort createdAt DESC mi?
        Sort.Order order = used.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());

        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("search(): sort='username,asc' verilirse Sort username ASC olmalı")
    void search_shouldParseSortAsc() {
        // Arrange
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        // Act
        auditLogService.search(
                null, null, null,
                null, null,
                1, 10,
                "username,asc"
        );

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        assertEquals(1, used.getPageNumber());
        assertEquals(10, used.getPageSize());

        Sort.Order order = used.getSort().getOrderFor("username");
        assertNotNull(order);
        assertEquals(Sort.Direction.ASC, order.getDirection());

        verifyNoMoreInteractions(auditLogRepository);
    }

    @Test
    @DisplayName("search(): sort='createdAt,desc' verilirse Sort createdAt DESC olmalı")
    void search_shouldParseSortDesc() {
        // Arrange
        when(auditLogRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(Page.empty());

        // Act
        auditLogService.search(
                null, null, null,
                null, null,
                0, 20,
                "createdAt,desc"
        );

        // Assert
        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(auditLogRepository).findAll(any(Specification.class), pageableCaptor.capture());

        Pageable used = pageableCaptor.getValue();
        Sort.Order order = used.getSort().getOrderFor("createdAt");
        assertNotNull(order);
        assertEquals(Sort.Direction.DESC, order.getDirection());

        verifyNoMoreInteractions(auditLogRepository);
    }
}
