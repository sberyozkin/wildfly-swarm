package org.jboss.unimbus.servlet.undertow;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Any;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.BeanManager;
import javax.inject.Inject;
import javax.servlet.ServletException;

import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.servlet.api.ServletContainer;
import org.jboss.unimbus.events.LifecycleEvent;
import org.jboss.unimbus.servlet.DeploymentMetaData;
import org.jboss.unimbus.servlet.Deployments;
import org.jboss.unimbus.servlet.undertow.util.DeploymentUtils;
import org.jboss.weld.environment.servlet.WeldServletLifecycle;

/**
 * Created by bob on 1/17/18.
 */
@ApplicationScoped
public class Deployer {

    void deploy(@Observes LifecycleEvent.Deploy event) {
        for (DeploymentMetaData each : this.deployments) {
            if (each == null) {
                continue;
            }
            DeploymentInfo info = DeploymentUtils.convert(each);
            info.addServletContextAttribute(WeldServletLifecycle.BEAN_MANAGER_ATTRIBUTE_NAME, this.beanManager);
            DeploymentManager manager = this.container.addDeployment(info);
            this.managers.add(manager);
            manager.deploy();
            try {
                HttpHandler handler = manager.start();
                root.addPrefixPath(info.getContextPath(), handler);
                this.handlers.add(handler);
            } catch (ServletException e) {
                e.printStackTrace();
            }
        }
    }

    void announce(@Observes LifecycleEvent.AfterStart event) {
        for (DeploymentMetaData each : this.deployments) {
            System.err.println(each.getName() + " mounted at " + each.getContextPath());
        }
    }

    @Inject
    PathHandler root;

    @Inject
    ServletContainer container;

    @Inject
    Deployments deployments;

    private List<DeploymentManager> managers = new ArrayList<>();

    private List<HttpHandler> handlers = new ArrayList<>();

    @Inject
    BeanManager beanManager;
}