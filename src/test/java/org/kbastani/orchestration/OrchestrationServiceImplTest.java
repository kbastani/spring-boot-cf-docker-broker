package org.kbastani.orchestration;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.kbastani.ServiceBrokerApplication;
import org.kbastani.model.DeploymentRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.boot.test.WebIntegrationTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(ServiceBrokerApplication.class)
@WebIntegrationTest
@ActiveProfiles("test")
public class OrchestrationServiceImplTest {

    @Autowired
    OrchestrationService orchestrationService;

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testDeploy() throws Exception {
        orchestrationService.deploy(new DeploymentRequest());
    }
}