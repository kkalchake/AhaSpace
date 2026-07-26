package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Section;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SectionRepository extends JpaRepository<Section, Long> {
    List<Section> findByPhaseIdOrderByOrderIndexAsc(Long phaseId);
}
