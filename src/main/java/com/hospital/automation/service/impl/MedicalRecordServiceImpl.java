package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.MedicalRecordCreateRequest;
import com.hospital.automation.domain.dto.response.MedicalRecordResponse;
import com.hospital.automation.domain.entity.MedicalRecord;
import com.hospital.automation.domain.entity.Visit;
import com.hospital.automation.repository.MedicalRecordRepository;
import com.hospital.automation.repository.VisitRepository;
import com.hospital.automation.service.MedicalRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VisitRepository visitRepository;

    @Override
    public MedicalRecordResponse create(MedicalRecordCreateRequest request) {
        Visit visit = visitRepository.findById(request.visitId())
                .orElseThrow(() -> new NotFoundException("Visit not found: " + request.visitId()));

        MedicalRecord record = MedicalRecord.builder()
                .visit(visit)
                .recordType(request.recordType())
                .content(request.content())
                .createdAt(LocalDateTime.now())
                .build();

        MedicalRecord saved = medicalRecordRepository.save(record);
        return new MedicalRecordResponse(saved.getId(), saved.getRecordType(), saved.getContent(), saved.getCreatedAt());
    }
}
