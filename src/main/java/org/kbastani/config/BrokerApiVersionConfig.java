package org.kbastani.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.kbastani.interceptor.BrokerApiVersionInterceptor;
import org.kbastani.model.BrokerApiVersion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import java.util.List;

@Configuration
@EnableWebMvc
public class BrokerApiVersionConfig extends WebMvcConfigurerAdapter {

    @Autowired
    private BrokerApiVersion brokerApiVersion;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new BrokerApiVersionInterceptor(brokerApiVersion)).addPathPatterns("/v2/**");
    }

    @Override
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {
        converters.add(new MappingJackson2HttpMessageConverter(objectMapper));
        super.configureMessageConverters(converters);
    }
}