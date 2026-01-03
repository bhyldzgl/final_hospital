package com.hospital.automation.service;

import com.hospital.automation.common.exception.NotFoundException;
import com.hospital.automation.domain.dto.request.PrescriptionCreateRequest;
import com.hospital.automation.domain.dto.request.PrescriptionItemCreateRequest;
import com.hospital.automation.domain.dto.response.PrescriptionResponse;
import com.hospital.automation.domain.entity.Prescription;
import com.hospital.automation.domain.entity.PrescriptionItem;
import com.hospital.automation.domain.entity.Visit;
import com.hospital.automation.repository.PrescriptionRepository;
import com.hospital.automation.repository.VisitRepository;
import com.hospital.automation.service.impl.PrescriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PrescriptionServiceImplTest {

    @Mock
    private PrescriptionRepository prescriptionRepository;

    @Mock
    private VisitRepository visitRepository;

    @InjectMocks
    private PrescriptionServiceImpl prescriptionService;

    private Visit visit;

    @BeforeEach
    void setUp() {
        visit = Visit.builder()
                .id(10L)
                .visitTime(LocalDateTime.now().minusHours(1))
                .build();
    }

    @Test
    void create_shouldThrowNotFound_whenVisitNotExists() {
        // given
        PrescriptionCreateRequest req = new PrescriptionCreateRequest(
                999L,
                "note",
                List.of(
                        new PrescriptionItemCreateRequest("Paracetamol", "500mg", "2x", 3, "tok")
                )
        );

        when(visitRepository.findById(999L)).thenReturn(Optional.empty());

        // when + then
        NotFoundException ex = assertThrows(NotFoundException.class, () -> prescriptionService.create(req));
        assertEquals("Visit not found: 999", ex.getMessage());

        verify(prescriptionRepository, never()).save(any());
    }

    @Test
    void create_shouldSavePrescription_withoutItems_whenItemsNull() {
        // given
        PrescriptionCreateRequest req = new PrescriptionCreateRequest(10L, "only note", null);

        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));

        // repo save dönüşü (id setlenmiş + createdAt korunmuş gibi)
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(inv -> {
                    Prescription p = inv.getArgument(0);
                    p.setId(77L);
                    return p;
                });

        LocalDateTime before = LocalDateTime.now();

        // when
        PrescriptionResponse res = prescriptionService.create(req);

        LocalDateTime after = LocalDateTime.now();

        // then
        assertNotNull(res);
        assertEquals(77L, res.id());
        assertEquals("only note", res.note());
        assertNotNull(res.createdAt());
        assertTrue(res.items().isEmpty());

        // createdAt servis içinde now() -> makul aralık kontrolü
        assertFalse(res.createdAt().isBefore(before.minusSeconds(1)));
        assertFalse(res.createdAt().isAfter(after.plusSeconds(1)));

        // save'e giden entity doğrulama
        ArgumentCaptor<Prescription> captor = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionRepository).save(captor.capture());

        Prescription savedArg = captor.getValue();
        assertNotNull(savedArg.getVisit());
        assertEquals(10L, savedArg.getVisit().getId());
        assertEquals("only note", savedArg.getNote());
        assertNotNull(savedArg.getCreatedAt());
        assertNotNull(savedArg.getItems());
        assertEquals(0, savedArg.getItems().size());
    }

    @Test
    void create_shouldSavePrescription_withItems_andReturnMappedResponse() {
        // given
        var item1 = new PrescriptionItemCreateRequest("Amoxicillin", "500mg", "3x", 7, "tok");
        var item2 = new PrescriptionItemCreateRequest("Ibuprofen", "200mg", "2x", 5, "aç");

        PrescriptionCreateRequest req = new PrescriptionCreateRequest(
                10L,
                "doctor note",
                List.of(item1, item2)
        );

        when(visitRepository.findById(10L)).thenReturn(Optional.of(visit));

        // save: id ve item id’leri atanmış gibi davranalım
        when(prescriptionRepository.save(any(Prescription.class)))
                .thenAnswer(inv -> {
                    Prescription p = inv.getArgument(0);
                    p.setId(55L);

                    long itemId = 1L;
                    for (PrescriptionItem it : p.getItems()) {
                        it.setId(itemId++);
                    }
                    return p;
                });

        LocalDateTime before = LocalDateTime.now();

        // when
        PrescriptionResponse res = prescriptionService.create(req);

        LocalDateTime after = LocalDateTime.now();

        // then (response)
        assertNotNull(res);
        assertEquals(55L, res.id());
        assertEquals("doctor note", res.note());
        assertNotNull(res.createdAt());
        assertEquals(2, res.items().size());

        assertFalse(res.createdAt().isBefore(before.minusSeconds(1)));
        assertFalse(res.createdAt().isAfter(after.plusSeconds(1)));

        // item mapping kontrolü (sıra korunuyor: list.stream().map...)
        assertEquals("Amoxicillin", res.items().get(0).drugName());
        assertEquals("500mg", res.items().get(0).dosage());
        assertEquals("3x", res.items().get(0).frequency());
        assertEquals(7, res.items().get(0).durationDays());
        assertEquals("tok", res.items().get(0).instructions());
        assertNotNull(res.items().get(0).id());

        assertEquals("Ibuprofen", res.items().get(1).drugName());
        assertEquals("200mg", res.items().get(1).dosage());
        assertEquals("2x", res.items().get(1).frequency());
        assertEquals(5, res.items().get(1).durationDays());
        assertEquals("aç", res.items().get(1).instructions());
        assertNotNull(res.items().get(1).id());

        // save'e giden entity kontrolü
        ArgumentCaptor<Prescription> captor = ArgumentCaptor.forClass(Prescription.class);
        verify(prescriptionRepository).save(captor.capture());

        Prescription arg = captor.getValue();
        assertNotNull(arg.getVisit());
        assertEquals(10L, arg.getVisit().getId());
        assertEquals("doctor note", arg.getNote());
        assertNotNull(arg.getCreatedAt());

        assertNotNull(arg.getItems());
        assertEquals(2, arg.getItems().size());

        PrescriptionItem a = arg.getItems().get(0);
        assertSame(arg, a.getPrescription(), "Item should reference parent Prescription");
        assertEquals("Amoxicillin", a.getDrugName());
        assertEquals("500mg", a.getDosage());
        assertEquals("3x", a.getFrequency());
        assertEquals(7, a.getDurationDays());
        assertEquals("tok", a.getInstructions());

        PrescriptionItem b = arg.getItems().get(1);
        assertSame(arg, b.getPrescription(), "Item should reference parent Prescription");
        assertEquals("Ibuprofen", b.getDrugName());
        assertEquals("200mg", b.getDosage());
        assertEquals("2x", b.getFrequency());
        assertEquals(5, b.getDurationDays());
        assertEquals("aç", b.getInstructions());
    }
}
