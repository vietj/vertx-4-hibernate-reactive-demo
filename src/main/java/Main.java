import io.vertx.mutiny.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
/*
    FixedHostPortGenericContainer<?> container = new FixedHostPortGenericContainer<>(PostgreSQLContainer.IMAGE + ":" + PostgreSQLContainer.DEFAULT_TAG)
      .withFixedExposedPort(5432, 5432);
    container.addEnv("POSTGRES_DB", "postgres");
    container.addEnv("POSTGRES_USER", "postgres");
    container.addEnv("POSTGRES_PASSWORD", "vertx-in-action");
    container.start();
*/

    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new ApiVerticle()).subscribe().with(
      ok -> logger.info("ApiVerticle was deployed successfully"),
      err -> logger.error("ApiVerticle deployment failed", err));
  }
}
