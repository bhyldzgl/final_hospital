package com.hospital.automation.controller;

import com.hospital.automation.domain.dto.request.VisitCreateRequest;
import com.hospital.automation.domain.dto.response.VisitResponse;
import com.hospital.automation.service.VisitService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/visits")
@PreAuthorize("hasAnyRole('ADMIN','DOCTOR')")
public class VisitController {

    private final VisitService visitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponse create(@Valid @RequestBody VisitCreateRequest request) {
        return visitService.create(request);
    }

    @GetMapping
    public List<VisitResponse> getAll() {
        return visitService.getAll();
    }

    @GetMapping("/{id}")
    public VisitResponse getById(@PathVariable Long id) {
        return visitService.getById(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        visitService.delete(id);
    }
}
