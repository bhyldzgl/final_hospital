package com.hospital.automation.service.impl;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.PrescriptionCreateRequest;
import com.hospital.automation.domain.dto.response.PrescriptionItemResponse;
import com.hospital.automation.domain.dto.response.PrescriptionResponse;
import com.hospital.automation.domain.entity.Prescription;
import com.hospital.automation.domain.entity.PrescriptionItem;
import com.hospital.automation.domain.entity.Visit;
import com.hospital.automation.repository.PrescriptionRepository;
import com.hospital.automation.repository.VisitRepository;
import com.hospital.automation.service.PrescriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
@Transactional
public class PrescriptionServiceImpl implements PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final VisitRepository visitRepository;

    @Override
    public PrescriptionResponse create(PrescriptionCreateRequest request) {
        Visit visit = visitRepository.findById(request.visitId())
                .orElseThrow(() -> new NotFoundException("Visit not found: " + request.visitId()));

        Prescription prescription = Prescription.builder()
                .visit(visit)
                .createdAt(LocalDateTime.now())
                .note(request.note())
                .items(new ArrayList<>())
                .build();

        if (request.items() != null) {
            for (var itemReq : request.items()) {
                PrescriptionItem item = PrescriptionItem.builder()
                        .prescription(prescription)
                        .drugName(itemReq.drugName())
                        .dosage(itemReq.dosage())
                        .frequency(itemReq.frequency())
                        .durationDays(itemReq.durationDays())
                        .instructions(itemReq.instructions())
                        .build();
                prescription.getItems().add(item);
            }
        }

        Prescription saved = prescriptionRepository.save(prescription);

        return new PrescriptionResponse(
                saved.getId(),
                saved.getCreatedAt(),
                saved.getNote(),
                saved.getItems().stream()
                        .map(i -> new PrescriptionItemResponse(i.getId(), i.getDrugName(), i.getDosage(), i.getFrequency(), i.getDurationDays(), i.getInstructions()))
                        .toList()
        );
    }
}
