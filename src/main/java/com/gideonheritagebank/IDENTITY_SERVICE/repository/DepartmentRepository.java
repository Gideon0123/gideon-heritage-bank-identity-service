package com.gideonheritagebank.IDENTITY_SERVICE.repository;

import com.gideonheritagebank.IDENTITY_SERVICE.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    Optional<Department> findByDepartmentCodeIgnoreCase(String departmentCode);

    boolean existsByDepartmentCodeIgnoreCase(String departmentCode);

    boolean existsByDepartmentCodeIgnoreCaseAndIdNot(
            String departmentCode,
            UUID id
    );

    Page<Department> findAllByOrderByNameAsc(Pageable pageable);

    Page<Department> findAllByActiveTrueOrderByNameAsc(Pageable pageable);

    Page<Department> findAllByActive(boolean active, Pageable pageable);
}
