import io.vertx.core.DeploymentOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.PostgreSQLContainer;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {

    logger.info("🚀 Starting a PostgreSQL container");

    PostgreSQLContainer<?> postgreSQLContainer = new PostgreSQLContainer<>("postgres:11-alpine")
      .withDatabaseName("postgres")
      .withUsername("postgres")
      .withPassword("vertx-in-action");
    postgreSQLContainer.start();

    logger.info("🚀 Starting Vert.x");

    Vertx vertx = Vertx.vertx();

    DeploymentOptions options = new DeploymentOptions();
    options.setConfig(new JsonObject()
      .put("pgPort", postgreSQLContainer.getMappedPort(5432)));

    vertx.deployVerticle(new ApiVerticle(), options).subscribe().with(
      ok -> logger.info("✅ ApiVerticle was deployed successfully"),
      err -> logger.error("🔥 ApiVerticle deployment failed", err));
  }
}
