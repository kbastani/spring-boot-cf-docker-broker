package org.kbastani.orchestration;

import org.kbastani.model.DeploymentRequest;

/**
 * Provides orchestration services and diego container orchestration on Cloud Foundry.
 *
 * @author kbastani
 */
public interface OrchestrationService {

    void deploy(DeploymentRequest deploymentRequest);
}
