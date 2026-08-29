package com.onest.app.security.repository;

import com.onest.app.security.model.AppMenu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppMenuRepository extends JpaRepository<AppMenu, Long> {

    List<AppMenu> findByActiveOrderByOrderNo(String active);
}
