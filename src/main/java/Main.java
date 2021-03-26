import io.vertx.mutiny.core.Vertx;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main {

  private static final Logger logger = LoggerFactory.getLogger(Main.class);

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();

    vertx.deployVerticle(new ApiVerticle()).subscribe().with(
      ok -> logger.info("ApiVerticle was deployed successfully"),
      err -> logger.error("ApiVerticle deployment failed", err));
  }
}
