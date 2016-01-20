package org.kbastani.repositories;

import org.kbastani.catalog.ServiceInstanceBinding;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ServiceInstanceBindingRepository extends JpaRepository<ServiceInstanceBinding, String> {
}
