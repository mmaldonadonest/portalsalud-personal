package com.onest.app.security.repository;

import com.onest.app.security.model.AppMenuRole;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppMenuRoleRepository extends JpaRepository<AppMenuRole, Long> {

    @Query("""
            select mr from AppMenuRole mr
            join fetch mr.menu m
            where mr.role.id = :roleId and m.active = 'Y'
            order by m.orderNo
            """)
    List<AppMenuRole> findActiveByRoleId(@Param("roleId") Long roleId);

    Optional<AppMenuRole> findByRoleIdAndMenuId(Long roleId, Long menuId);

    boolean existsByRoleId(Long roleId);
}
