package com.helpdeskAi.org.service;

import com.helpdeskAi.common.exception.ResourceNotFoundException;
import com.helpdeskAi.common.security.TenantContext;
import com.helpdeskAi.org.dto.*;
import com.helpdeskAi.org.model.*;
import com.helpdeskAi.org.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DepartmentService {

    private final DepartmentRepository departmentRepository;

    public List<DepartmentResponse> listDepartments() {
        UUID orgId = TenantContext.getTenantId();
        return departmentRepository.findByOrganizationIdOrderByNameAsc(orgId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public DepartmentResponse createDepartment(CreateDepartmentRequest request) {
        UUID orgId = TenantContext.getTenantId();
        Department dept = Department.builder()
                .organizationId(orgId)
                .name(request.getName())
                .description(request.getDescription())
                .build();
        dept = departmentRepository.save(dept);
        log.info("Created department: {} for org: {}", dept.getName(), orgId);
        return toResponse(dept);
    }

    @Transactional
    public DepartmentResponse updateDepartment(UUID deptId, CreateDepartmentRequest request) {
        Department dept = departmentRepository.findById(deptId)
                .orElseThrow(() -> new ResourceNotFoundException("Department", "id", deptId));
        if (request.getName() != null) dept.setName(request.getName());
        if (request.getDescription() != null) dept.setDescription(request.getDescription());
        dept = departmentRepository.save(dept);
        return toResponse(dept);
    }

    @Transactional
    public void deleteDepartment(UUID deptId) {
        if (!departmentRepository.existsById(deptId)) {
            throw new ResourceNotFoundException("Department", "id", deptId);
        }
        departmentRepository.deleteById(deptId);
        log.info("Deleted department: {}", deptId);
    }

    private DepartmentResponse toResponse(Department d) {
        return DepartmentResponse.builder()
                .id(d.getId())
                .organizationId(d.getOrganizationId())
                .name(d.getName())
                .description(d.getDescription())
                .createdAt(d.getCreatedAt())
                .build();
    }
}
