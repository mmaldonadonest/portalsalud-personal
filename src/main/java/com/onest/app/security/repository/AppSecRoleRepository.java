package com.onest.app.security.repository;

import com.onest.app.security.model.AppSecRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSecRoleRepository extends JpaRepository<AppSecRole, Long> {

    List<AppSecRole> findByActiveOrderByName(String active);

    List<AppSecRole> findAllByOrderByNameAsc();

    Optional<AppSecRole> findByCode(String code);

    boolean existsByCode(String code);
}
