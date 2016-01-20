package org.kbastani.service;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.kbastani.catalog.ServiceInstanceBinding;
import org.kbastani.exception.ServiceBrokerException;
import org.kbastani.exception.ServiceInstanceBindingExistsException;
import org.kbastani.model.CreateServiceInstanceBindingRequest;
import org.kbastani.model.DeleteServiceInstanceBindingRequest;
import org.kbastani.repositories.ServiceInstanceBindingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class ServiceInstanceBindingServiceImpl implements ServiceInstanceBindingService {

    Log log = LogFactory.getLog(ServiceInstanceBindingService.class);

    private ServiceInstanceBindingRepository serviceInstanceBindingRepository;

    @Autowired
    public ServiceInstanceBindingServiceImpl(ServiceInstanceBindingRepository serviceInstanceBindingRepository) {
        this.serviceInstanceBindingRepository = serviceInstanceBindingRepository;
    }

    /**
     * Create a new binding to a service instance.
     *
     * @param createServiceInstanceBindingRequest containing parameters sent from Cloud Controller
     * @return The newly created ServiceInstanceBinding
     * @throws ServiceInstanceBindingExistsException if the same binding already exists
     * @throws ServiceBrokerException                on internal failure
     */
    @Override
    public ServiceInstanceBinding createServiceInstanceBinding(CreateServiceInstanceBindingRequest createServiceInstanceBindingRequest) throws ServiceInstanceBindingExistsException, ServiceBrokerException {

        ServiceInstanceBinding serviceInstanceBinding = serviceInstanceBindingRepository.findOne(createServiceInstanceBindingRequest.getBindingId());

        if (serviceInstanceBinding != null)
            throw new ServiceInstanceBindingExistsException(serviceInstanceBinding);

        // Test credentials
        Map<String, String> credentials;
        credentials = new HashMap<>();
        credentials.put("uri", "uri");
        credentials.put("username", "username");
        credentials.put("password", "password");

        serviceInstanceBinding = new ServiceInstanceBinding(createServiceInstanceBindingRequest.getBindingId(),
                createServiceInstanceBindingRequest.getServiceInstanceId(),
                credentials, null, createServiceInstanceBindingRequest.getAppGuid());

        try {
            serviceInstanceBinding = serviceInstanceBindingRepository.save(serviceInstanceBinding);
        } catch (Exception ex) {
            log.error(ex);
            throw new ServiceBrokerException(ex);
        }

        return serviceInstanceBinding;
    }

    /**
     * Delete the service instance binding. If a binding doesn't exist,
     * return null.
     *
     * @param deleteServiceInstanceBindingRequest containing parameters sent from Cloud Controller
     * @return The deleted ServiceInstanceBinding or null if one does not exist
     * @throws ServiceBrokerException on internal failure
     */
    @Override
    public ServiceInstanceBinding deleteServiceInstanceBinding(DeleteServiceInstanceBindingRequest deleteServiceInstanceBindingRequest) throws ServiceBrokerException {
        ServiceInstanceBinding serviceInstanceBinding;

        if (!serviceInstanceBindingRepository.exists(deleteServiceInstanceBindingRequest.getBindingId())) {
            throw new ServiceBrokerException(String.format("Service instance binding does not exist: %s", deleteServiceInstanceBindingRequest.getBindingId()));
        } else {
            serviceInstanceBinding = serviceInstanceBindingRepository.findOne(deleteServiceInstanceBindingRequest.getBindingId());
        }

        try {
            serviceInstanceBindingRepository.delete(deleteServiceInstanceBindingRequest.getBindingId());
        } catch (Exception ex) {
            log.error(ex);
        }

        return serviceInstanceBinding;
    }
}
