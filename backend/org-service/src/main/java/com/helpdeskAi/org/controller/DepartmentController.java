package com.helpdeskAi.org.controller;

import com.helpdeskAi.common.dto.ApiResponse;
import com.helpdeskAi.org.dto.*;
import com.helpdeskAi.org.service.DepartmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/org/departments")
@RequiredArgsConstructor
public class DepartmentController {

    private final DepartmentService departmentService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DepartmentResponse>>> listDepartments() {
        List<DepartmentResponse> depts = departmentService.listDepartments();
        return ResponseEntity.ok(new ApiResponse<>(true, "Departments retrieved", depts, Instant.now()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse dept = departmentService.createDepartment(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(true, "Department created", dept, Instant.now()));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID id, @Valid @RequestBody CreateDepartmentRequest request) {
        DepartmentResponse dept = departmentService.updateDepartment(id, request);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department updated", dept, Instant.now()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(@PathVariable UUID id) {
        departmentService.deleteDepartment(id);
        return ResponseEntity.ok(new ApiResponse<>(true, "Department deleted", null, Instant.now()));
    }
}
