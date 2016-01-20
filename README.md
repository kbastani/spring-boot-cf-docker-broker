# Spring Boot CF Docker Broker

_Spring Boot CF Docker Broker_ is a sample application that implements a Cloud Foundry service broker for orchestrating containers from a Docker registry inside a Spring Boot application.

This sample was extended from the [Spring Boot CF Service Broker](https://github.com/cloudfoundry-community/spring-boot-cf-service-broker) project template.

## Overview

The goal of this project is to provide developers with a way to create platform provided services using Cloud Foundry and _Spring Boot_. This project is mostly intended for tinkering. It's a good starter for developers who are already familiar with Spring Boot and would like to orchestrate containers for creating platform services from publicly available container distributions on Docker Hub.

### Compatibility

* [Cloud Controller Service Broker API](http://docs.cloudfoundry.org/services/api.html): 2.7
* [Cloud Foundry Release](https://github.com/cloudfoundry/cf-release): 227+
* [Pivotal CF](http://www.pivotal.io/platform-as-a-service/pivotal-cf): 1.6
