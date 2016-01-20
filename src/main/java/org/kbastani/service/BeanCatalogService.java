package org.kbastani.service;

import org.kbastani.catalog.Catalog;
import org.kbastani.catalog.ServiceDefinition;
import org.kbastani.repositories.ServiceDefinitionRepository;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * An implementation of the CatalogService that gets the catalog injected (ie configure 
 * in spring config)
 * 
 * @author sgreenberg@gopivotal.com
 *
 */
public class BeanCatalogService implements CatalogService {

	private Catalog catalog;
    private ServiceDefinitionRepository serviceDefinitionRepository;

	@Autowired
	public BeanCatalogService(Catalog catalog, ServiceDefinitionRepository serviceDefinitionRepository) {
		this.catalog = catalog;
        this.serviceDefinitionRepository = serviceDefinitionRepository;
	}

	@Override
	public Catalog getCatalog() {
        catalog.getServices();
		return catalog;
	}

	@Override
	public ServiceDefinition getServiceDefinition(String serviceId) {
		return serviceDefinitionRepository.findOne(serviceId);
	}

}
