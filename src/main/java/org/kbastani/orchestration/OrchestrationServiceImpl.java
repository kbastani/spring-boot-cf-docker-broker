package org.kbastani.orchestration;


import org.cloudfoundry.client.spring.SpringCloudFoundryClient;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsRequest;
import org.cloudfoundry.client.v2.organizations.ListOrganizationsResponse;
import org.cloudfoundry.client.v2.routes.CreateRouteRequest;
import org.cloudfoundry.client.v2.routes.CreateRouteResponse;
import org.cloudfoundry.client.v2.routes.ListRoutesRequest;
import org.cloudfoundry.client.v2.routes.ListRoutesResponse;
import org.cloudfoundry.client.v3.applications.*;
import org.cloudfoundry.client.v3.droplets.GetDropletRequest;
import org.cloudfoundry.client.v3.droplets.GetDropletResponse;
import org.cloudfoundry.client.v3.packages.CreatePackageRequest;
import org.cloudfoundry.client.v3.packages.CreatePackageResponse;
import org.cloudfoundry.client.v3.packages.StagePackageRequest;
import org.cloudfoundry.client.v3.packages.StagePackageResponse;
import org.kbastani.model.DeploymentRequest;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.rx.Stream;
import rx.Observable;
import rx.Subscriber;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;


@Service
public class OrchestrationServiceImpl implements OrchestrationService {

    public static final String spaceId = "25259f88-e3b4-442c-8f18-accb90497bb0";
    public static final String dockerImage = "neo4j";
    public static final String domainId = "f5759934-5053-4122-96c4-822c9cfa15d5";
    private final Logger logger = LoggerFactory.getLogger(OrchestrationServiceImpl.class);
    private final String APP_NAME = "neo4j";

    private SpringCloudFoundryClient cloudFoundryClient;

    @Autowired
    public OrchestrationServiceImpl(SpringCloudFoundryClient springCloudFoundryClient) {
        this.cloudFoundryClient = springCloudFoundryClient;
    }

    @Override
    public void deploy(DeploymentRequest deploymentRequest) {
        Map<String, String> organizations = new HashMap<>();

        ListOrganizationsResponse listOrganizations = cloudFoundryClient.organizations()
                .list(ListOrganizationsRequest.builder().name("kbastani").build()).get();

        listOrganizations
                .getResources()
                .stream()
                .forEach(r -> {
                    String key = r.getMetadata().getId();
                    String value = r.getEntity().getName();
                    organizations.put(key, value);
                });

        this.logger.info("{} Organizations Found", organizations);

        Map<String, String> applications = new HashMap<>();

        ListApplicationsResponse listApplications = cloudFoundryClient.applicationsV3()
                .list(ListApplicationsRequest.builder()
                        .build()).get();

        listApplications.getResources()
                .forEach(r -> {
                    String key = r.getId();
                    String value = r.getName();
                    applications.put(key, value);
                });

        // Deploy a container
        this.logger.info("{} Applications Found", applications);

        // Delete application
        if (listApplications.getResources().size() > 0) {
            listApplications.getResources()
                    .stream()
                    .filter(app -> app.getName().equals(APP_NAME))
                    .forEach(r -> {
                        logger.info("Deleting application '{}'...", r.getName());
                        // Delete the application
                        cloudFoundryClient.applicationsV3()
                                .delete(DeleteApplicationRequest.builder().id(r.getId()).build())
                                .get();
                    });
        }

        Map<String, Object> lifecycle = new HashMap<>();
        lifecycle.put("type", "docker");
        Map<String, Object> dataMap = new HashMap<>();
        lifecycle.put("data", dataMap);

        // Push a container
        CreateApplicationRequest dockerImage = CreateApplicationRequest.builder()
                .name(APP_NAME)
                .spaceId(spaceId)
                .environmentVariable("open", "source")
                .lifecycle(lifecycle)
                .build();

        Map<String, Object> dockerResult = new HashMap<>();

        CreateApplicationResponse application =
                observeWithRetry(t -> Stream.just(this.cloudFoundryClient.applicationsV3().create(t)), dockerImage);

        dockerResult.put(application.getId(), application);
        this.logger.info("{} Created Application", dockerResult);

        Map<String, String> data = new HashMap<>();
        data.put("image", OrchestrationServiceImpl.dockerImage);

        CreatePackageRequest createPackageRequest = CreatePackageRequest.builder()
                .applicationId(dockerResult.keySet().stream().findFirst().get())
                .type(CreatePackageRequest.PackageType.DOCKER)
                .data(data)
                .build();

        Map<String, CreatePackageResponse> packageRequestMap = new HashMap<>();

        CreatePackageResponse createPackageResponse =
                observeWithRetry(t -> Stream.just(cloudFoundryClient.packages().create(t)), createPackageRequest);

        packageRequestMap.put(createPackageResponse.getId(), createPackageResponse);

        this.logger.info("{} Created Container", packageRequestMap);

        Map<String, StagePackageResponse> stagePackageResponseMap = new HashMap<>();

        StagePackageResponse stagePackageResponse = stagePackage(packageRequestMap.values().stream().findFirst().get());
        stagePackageResponseMap.put(stagePackageResponse.getId(), stagePackageResponse);
        this.logger.info("{} Staged Container", stagePackageResponseMap);

        GetDropletResponse stageDropletResponse = null;

        // Wait until package is in a staged state
        while (stageDropletResponse == null || !Objects.equals(stageDropletResponse.getState(), "STAGED")) {
            GetDropletRequest getDropletRequest = GetDropletRequest.builder()
                    .id(stagePackageResponse.getId())
                    .build();

            stageDropletResponse =
                    observeWithRetry(t -> Stream.just(cloudFoundryClient.droplets().get(t)), getDropletRequest);

            try {
                logger.info("Staging package...");
                Thread.sleep(3000L);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        // Assign current droplet
        AssignApplicationDropletRequest assignDropletRequest = AssignApplicationDropletRequest.builder()
                .dropletId(stagePackageResponseMap.keySet().stream().findFirst().get())
                .id(dockerResult.keySet().stream().findFirst().get())
                .build();

        AssignApplicationDropletResponse dropletResponse =
                observeWithRetry(t -> Stream.just(cloudFoundryClient.applicationsV3().assignDroplet(t)), assignDropletRequest);

        this.logger.info("{} Assigned Droplet", dropletResponse);

        // Update the health check flag
        org.cloudfoundry.client.v2.applications.ListApplicationsResponse listApplicationsResponse =
                observeWithRetry(t -> Stream.just(cloudFoundryClient.applicationsV2().list(t)),
                        org.cloudfoundry.client.v2.applications.ListApplicationsRequest
                                .builder().name(String.format("v3-%s-web", APP_NAME)).build());

        String v2AppId = listApplicationsResponse.getResources().stream()
                .findFirst().get().getMetadata().getId();

        org.cloudfoundry.client.v2.applications.UpdateApplicationResponse updateApplicationResponse =
                observeWithRetry(t -> Stream.just(cloudFoundryClient.applicationsV2().update(t)),
                        org.cloudfoundry.client.v2.applications.UpdateApplicationRequest.builder()
                                .id(v2AppId)
                                .healthCheckType("none").build());

        this.logger.info("Updated health check type: {}", updateApplicationResponse);

        StartApplicationResponse startResult = observeWithRetry(t -> Stream.just(cloudFoundryClient.applicationsV3().start(t)),
                StartApplicationRequest.builder().id(dockerResult.keySet().stream().findFirst().get()).build());

        this.logger.info("{} Started Application", startResult);

        // Get route
        ListRoutesResponse listRoutesResponse =
                observeWithRetry(t -> Stream.just(cloudFoundryClient.routes().list(t)),
                        ListRoutesRequest.builder().host(APP_NAME).build());

        if (listRoutesResponse.getTotalResults() > 0) {
            listRoutesResponse.getResources().forEach(route -> {
                logger.info("Mapping route '{}' to application '{}'...", route.getMetadata().getUrl(), application.getId());
                mapApplicationRoute(route.getMetadata().getId(), application.getId());
            });
        } else {
            logger.info("Creating new route '{}'...", APP_NAME);

            CreateRouteResponse createRouteResponse = observeWithRetry(t -> Stream.just(cloudFoundryClient.routes().create(t)), CreateRouteRequest.builder()
                    .host(APP_NAME)
                    .domainId(domainId)
                    .spaceId(spaceId).build());

            logger.info("Mapping route '{}' to application '{}'...", createRouteResponse.getMetadata().getUrl(), application.getId());
            mapApplicationRoute(createRouteResponse.getMetadata().getId(), application.getId());
        }
    }

    /**
     * Performs single operations and returns a result that is mapped from a {@link Mono<U>} and request object.
     * Retries operations that fail from the {@link SpringCloudFoundryClient} operations.
     *
     * @param operationExecutor the function to apply to the request parameter
     * @param request           the request parameter that will be the argument of the operationExecutor
     * @param <T>               is the request object for a {@link SpringCloudFoundryClient} operation
     * @param <U>               is the response object for a {@link SpringCloudFoundryClient} operation based on {@linkplain T}
     * @return a {@linkplain U} that is the response object returned from a {@link SpringCloudFoundryClient} operation, or null after 3 retries
     */
    private <T, U> U observeWithRetry(Function<T, Publisher<Mono<U>>> operationExecutor, T request) {
        logger.info("Trying operation '{}'...", request.getClass().getSimpleName());
        return Observable.create((Subscriber<? super U> s) -> {
            try {
                s.onNext(Stream.from(operationExecutor.apply(request)).concatMap(responseStream -> responseStream).take(1).last().get());
            } catch (Exception ex) {
                logger.error(ex.getMessage());
                s.onError(ex);
            }
        }).retryWhen(a -> a.zipWith(Observable.range(1, 10), (n, i) -> i).flatMap(i -> {
            logger.info("Delay retry by " + i + " second(s)...");
            return Observable.timer(i, TimeUnit.SECONDS);
        })).toBlocking().first();
    }

    /**
     * Map an application to a route
     *
     * @param routeId       is the guid of the route
     * @param applicationId is the guid of the app
     */
    private void mapApplicationRoute(String routeId, String applicationId) {
        observeWithRetry(t -> Stream.just(cloudFoundryClient.applicationsV3().mapRoute(t)),
                MapApplicationRouteRequest.builder()
                        .id(applicationId)
                        .routeId(routeId)
                        .build());
    }

    /**
     * Stage a new package
     *
     * @param response is the response of package that was previously created
     * @return a result of the staging operation
     */
    private StagePackageResponse stagePackage(CreatePackageResponse response) {

        Map<String, Object> lifecycle = new HashMap<>();
        lifecycle.put("type", "docker");
        Map<String, String> data = new HashMap<>();
        lifecycle.put("data", data);

        StagePackageRequest request = StagePackageRequest.builder()
                .id(response.getId())
                .diskLimit(4096)
                .memoryLimit(1024)
                .environmentVariable("CUSTOM_ENV_VAR", "docker")
                .lifecycle(lifecycle)
                .build();

        return observeWithRetry(t -> Stream.just(this.cloudFoundryClient.packages().stage(t)), request);
    }
}
