package com.onest.app.security.repository;

import com.onest.app.security.model.AppSecUser;
import java.util.Optional;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSecUserRepository extends JpaRepository<AppSecUser, Long> {

    @Query("""
            select distinct u
            from AppSecUser u
            left join fetch u.roles r
            where (lower(u.username) = lower(:identifier) or lower(coalesce(u.email, '')) = lower(:identifier))
              and u.active = 'Y'
              and u.accountStatus = 'ACTIVE'
            """)
    Optional<AppSecUser> findActiveByIdentifier(@Param("identifier") String identifier);
}
