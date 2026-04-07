package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.DashboardSummaryDTO;
import com.bentorangel.finance_dashboard.dto.MonthlySummaryDTO;
import com.bentorangel.finance_dashboard.dto.TransactionRequestDTO;
import com.bentorangel.finance_dashboard.dto.TransactionResponseDTO;
import com.bentorangel.finance_dashboard.model.CategoryType;
import com.bentorangel.finance_dashboard.service.TransactionService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/transactions")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public ResponseEntity<TransactionResponseDTO> create(@Valid @RequestBody TransactionRequestDTO dto) {
        TransactionResponseDTO response = transactionService.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(uri).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<TransactionResponseDTO>> findAll(
            @ParameterObject
            @PageableDefault(sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(transactionService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TransactionResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody TransactionRequestDTO dto) {
        return ResponseEntity.ok(transactionService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        transactionService.delete(id);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // --- Endpoints Customizados para o Dashboard ---

    @GetMapping("/period")
    public ResponseEntity<Page<TransactionResponseDTO>> findByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject
            @PageableDefault(sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(transactionService.findByPeriod(startDate, endDate, pageable));
    }

    @GetMapping("/summary")
    public ResponseEntity<DashboardSummaryDTO> getSummary(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.ok(transactionService.getSummary(startDate, endDate));
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportToCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        byte[] csvData = transactionService.exportTransactionsToCsv(startDate, endDate);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=extrato_" + startDate + "_a_" + endDate + ".csv");
        headers.add(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

        return ResponseEntity.ok()
                .headers(headers)
                .body(csvData);
    }

    @GetMapping("/search")
    public ResponseEntity<Page<TransactionResponseDTO>> search(
            @RequestParam(required = false) String description,
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) CategoryType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @ParameterObject @PageableDefault(sort = "transactionDate", direction = Sort.Direction.DESC) Pageable pageable) {

        Page<TransactionResponseDTO> result = transactionService.searchTransactions(description, categoryId, type, startDate, endDate, pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/summary/monthly")
    public ResponseEntity<List<MonthlySummaryDTO>> getMonthlySummary(
            @RequestParam(required = false) Integer year) {

        int resolvedYear = (year != null) ? year : LocalDate.now().getYear();
        List<MonthlySummaryDTO> result = transactionService.getMonthlySummary(resolvedYear);
        return ResponseEntity.ok(result);
    }
}