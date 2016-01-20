package org.kbastani.config;

import org.kbastani.catalog.Catalog;
import org.kbastani.repositories.ServiceDefinitionRepository;
import org.kbastani.model.BrokerApiVersion;
import org.kbastani.service.BeanCatalogService;
import org.kbastani.service.CatalogService;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebMvcAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan
@ConditionalOnWebApplication
@AutoConfigureAfter({WebMvcAutoConfiguration.class, ServiceDefinitionRepository.class})
public class ServiceBrokerAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(BrokerApiVersion.class)
    public BrokerApiVersion brokerApiVersion() {
        return new BrokerApiVersion("2.7");
    }

    @Bean
    @ConditionalOnMissingBean(CatalogService.class)
    public CatalogService beanCatalogService(Catalog catalog, ServiceDefinitionRepository serviceDefinitionRepository) {
        return new BeanCatalogService(catalog, serviceDefinitionRepository);
    }
}