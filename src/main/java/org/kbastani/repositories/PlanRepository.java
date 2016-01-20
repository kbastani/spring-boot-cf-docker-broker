package org.kbastani.repositories;

import org.kbastani.catalog.Plan;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PlanRepository extends JpaRepository<Plan, String> {
}
