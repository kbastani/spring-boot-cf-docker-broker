package org.kbastani.repositories;

import org.kbastani.catalog.ServiceInstance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInstanceRepository extends JpaRepository<ServiceInstance, String> {
}
