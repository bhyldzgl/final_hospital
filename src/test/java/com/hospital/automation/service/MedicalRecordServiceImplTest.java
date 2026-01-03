package com.hospital.automation.service;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.MedicalRecordCreateRequest;
import com.hospital.automation.domain.dto.response.MedicalRecordResponse;
import com.hospital.automation.domain.entity.MedicalRecord;
import com.hospital.automation.domain.entity.Visit;
import com.hospital.automation.repository.MedicalRecordRepository;
import com.hospital.automation.repository.VisitRepository;
import com.hospital.automation.service.impl.MedicalRecordServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MedicalRecordServiceImplTest {

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private MedicalRecordServiceImpl medicalRecordService;

    @Test
    void create_shouldThrowNotFound_whenVisitNotExists() {
        // Arrange
        Long visitId = 10L;
        MedicalRecordCreateRequest request = new MedicalRecordCreateRequest(
                visitId,
                "LAB",
                "Blood test results..."
        );

        when(visitRepository.findById(visitId)).thenReturn(Optional.empty());

        // Act + Assert
        NotFoundException ex = assertThrows(NotFoundException.class,
                () -> medicalRecordService.create(request)
        );

        assertEquals("Visit not found: " + visitId, ex.getMessage());

        // Visit bulunamadığı için kayıt/save olmamalı
        verify(medicalRecordRepository, never()).save(any(MedicalRecord.class));
        verify(visitRepository).findById(visitId);
        verifyNoMoreInteractions(visitRepository, medicalRecordRepository);
    }

    @Test
    void create_shouldSaveAndReturnResponse_whenVisitExists() {
        // Arrange
        Long visitId = 10L;

        Visit visit = Visit.builder()
                .id(visitId)
                .build();

        MedicalRecordCreateRequest request = new MedicalRecordCreateRequest(
                visitId,
                "IMAGING",
                "CT scan report..."
        );

        when(visitRepository.findById(visitId)).thenReturn(Optional.of(visit));

        // save() çağrılınca service'in oluşturduğu objeyi yakalayıp id set edelim
        when(medicalRecordRepository.save(any(MedicalRecord.class))).thenAnswer(inv -> {
            MedicalRecord mr = inv.getArgument(0);
            mr.setId(100L); // DB’nin id verdiğini simüle ediyoruz
            return mr;
        });

        // createdAt'in "now" ile set edildiğini doğrulamak için zaman aralığı tutalım
        LocalDateTime before = LocalDateTime.now();

        // Act
        MedicalRecordResponse response = medicalRecordService.create(request);

        LocalDateTime after = LocalDateTime.now();

        // Assert - Response
        assertNotNull(response);
        assertEquals(100L, response.id());
        assertEquals("IMAGING", response.recordType());
        assertEquals("CT scan report...", response.content());
        assertNotNull(response.createdAt());

        // createdAt çok kritik: LocalDateTime.now() ile set edildi mi?
        // Çok küçük farklar olabileceği için güvenli aralık kontrolü yapıyoruz.
        assertFalse(response.createdAt().isBefore(before.minusSeconds(2)));
        assertFalse(response.createdAt().isAfter(after.plusSeconds(2)));

        // Assert - save'e giden entity doğru mu?
        ArgumentCaptor<MedicalRecord> captor = ArgumentCaptor.forClass(MedicalRecord.class);
        verify(medicalRecordRepository).save(captor.capture());
        MedicalRecord savedEntity = captor.getValue();

        assertNotNull(savedEntity);
        assertEquals("IMAGING", savedEntity.getRecordType());
        assertEquals("CT scan report...", savedEntity.getContent());
        assertNotNull(savedEntity.getCreatedAt());
        assertEquals(visitId, savedEntity.getVisit().getId());

        // Verify repository çağrıları
        verify(visitRepository).findById(visitId);
        verifyNoMoreInteractions(visitRepository, medicalRecordRepository);
    }
}
