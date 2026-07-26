package com.kkalchake.enlightenment.repository;

import com.kkalchake.enlightenment.model.Phase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhaseRepository extends JpaRepository<Phase, Long> {

    List<Phase> findByCourseIdOrderByOrderIndexAsc(Long courseId);

    boolean existsByTitle(String title);
}
