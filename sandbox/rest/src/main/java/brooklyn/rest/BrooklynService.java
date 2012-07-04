package brooklyn.rest;

import brooklyn.rest.commands.applications.DeleteApplicationCommand;
import brooklyn.rest.commands.applications.InvokeEffectorCommand;
import brooklyn.rest.commands.applications.ListApplicationsCommand;
import brooklyn.rest.commands.applications.ListEffectorsCommand;
import brooklyn.rest.commands.applications.QuerySensorsCommand;
import brooklyn.rest.commands.applications.StartApplicationCommand;
import brooklyn.rest.commands.catalog.ListCatalogEntitiesCommand;
import brooklyn.rest.commands.catalog.ListCatalogPoliciesCommand;
import brooklyn.rest.commands.catalog.ListConfigKeysCommand;
import brooklyn.rest.commands.catalog.LoadClassCommand;
import brooklyn.rest.commands.locations.AddLocationCommand;
import brooklyn.rest.commands.locations.ListLocationsCommand;
import brooklyn.rest.core.ApplicationManager;
import brooklyn.rest.core.LocationStore;
import brooklyn.rest.health.GeneralHealthCheck;
import brooklyn.rest.resources.ApplicationResource;
import brooklyn.rest.resources.CatalogResource;
import brooklyn.rest.resources.EffectorResource;
import brooklyn.rest.resources.EntityResource;
import brooklyn.rest.resources.LocationResource;
import brooklyn.rest.resources.SensorResource;
import brooklyn.rest.resources.SwaggerUiResource;
import com.yammer.dropwizard.Service;
import com.yammer.dropwizard.bundles.AssetsBundle;
import com.yammer.dropwizard.config.Environment;
import com.yammer.dropwizard.views.ViewBundle;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Application entry point. Configures and starts the embedded web-server.
 * Adds the commands the service exposes to the outside world.
 */
public class BrooklynService extends Service<BrooklynConfiguration> {

  protected BrooklynService() {
    super("brooklyn-rest");
    addBundle(new AssetsBundle("/swagger-ui"));
    addBundle(new ViewBundle());
  }

  @Override
  protected void initialize(BrooklynConfiguration configuration, Environment environment)
      throws Exception {

    // Create managed components and wire them together

    LocationStore locationStore = new LocationStore(configuration);
    environment.manage(locationStore);

    CatalogResource catalogResource = new CatalogResource();

    ExecutorConfiguration executorConfig = configuration.getExecutorConfiguration();
    ExecutorService managedExecutor = environment.managedExecutorService("brooklyn",
        executorConfig.getCorePoolSize(), executorConfig.getMaximumPoolSize(),
        executorConfig.getKeepAliveTimeInSeconds(), TimeUnit.SECONDS);

    ApplicationManager applicationManager = new ApplicationManager(configuration,
        locationStore, catalogResource, managedExecutor);
    environment.manage(applicationManager);

    environment.addResource(new LocationResource(locationStore));
    environment.addResource(catalogResource);

    environment.addResource(new ApplicationResource(applicationManager, locationStore, catalogResource));

    environment.addResource(new EntityResource(applicationManager));
    environment.addResource(new SensorResource(applicationManager));
    environment.addResource(new EffectorResource(applicationManager, managedExecutor));
    environment.addResource(new SwaggerUiResource());

    environment.addHealthCheck(new GeneralHealthCheck());
  }

  public static void main(String[] args) throws Exception {
    BrooklynService service = new BrooklynService();
    service.addCommand(new ListApplicationsCommand());
    service.addCommand(new StartApplicationCommand());
    service.addCommand(new DeleteApplicationCommand());

    service.addCommand(new QuerySensorsCommand());
    service.addCommand(new ListEffectorsCommand());
    service.addCommand(new InvokeEffectorCommand());

    service.addCommand(new ListLocationsCommand());
    service.addCommand(new AddLocationCommand());

    service.addCommand(new ListConfigKeysCommand());
    service.addCommand(new ListCatalogEntitiesCommand());
    service.addCommand(new ListCatalogPoliciesCommand());
    service.addCommand(new LoadClassCommand());

    service.run(args);
  }
}
