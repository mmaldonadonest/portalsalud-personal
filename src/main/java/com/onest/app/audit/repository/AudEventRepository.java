package com.onest.app.audit.repository;

import com.onest.app.audit.model.AudEvent;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface AudEventRepository extends JpaRepository<AudEvent, Long>, JpaSpecificationExecutor<AudEvent> {

    @Query("select distinct e.username from AudEvent e where e.username is not null order by e.username")
    List<String> usuarios();

    @Query("select distinct e.eventType from AudEvent e order by e.eventType")
    List<String> modulos();

    @Query("select distinct e.action from AudEvent e where e.action is not null order by e.action")
    List<String> acciones();
}
