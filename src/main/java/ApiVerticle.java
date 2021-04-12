import data.Product;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.core.http.HttpServer;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import org.hibernate.reactive.mutiny.Mutiny;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Persistence;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class ApiVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(ApiVerticle.class);

  private Mutiny.SessionFactory emf;

  public ApiVerticle() {
  }

  @Override
  public Uni<Void> asyncStart() {

    Uni<Void> startHibernate = Uni.createFrom().deferred(() -> {
      var pgPort = config().getInteger("pgPort", 5432);
      var props = Map.of("javax.persistence.jdbc.url", "jdbc:postgresql://localhost:" + pgPort + "/postgres");

      emf = Persistence
        .createEntityManagerFactory("pg-demo", props)
        .unwrap(Mutiny.SessionFactory.class);

      logger.info("Hibernate Reactive has started");
      return Uni.createFrom().voidItem();
    });

    startHibernate = vertx.executeBlocking(startHibernate);

    Router router = Router.router(vertx);

    BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler::handle);

    router.get("/products").respond(this::listProducts);
    router.get("/products/:id").respond(this::getProduct);
    router.post("/products").respond(this::createProduct);

    Uni<HttpServer> startHttpServer = vertx.createHttpServer()
      .requestHandler(router::handle)
      .listen(8080)
      .onItem().invoke(() -> logger.info("HTTP server listening on port 8080"));

    return Uni.combine().all().unis(startHibernate, startHttpServer).discardItems();
  }

  private Uni<List<Product>> listProducts(RoutingContext rc) {
    logger.info("listProducts");

    return emf.withSession(session -> session.createQuery("from Product", Product.class).getResultList());
  }

  private Uni<Product> createProduct(RoutingContext rc) {
    logger.info("createProduct");

    JsonObject json = rc.getBodyAsJson();
    String name;
    BigDecimal price;

    try {
      requireNonNull(json, "The incoming JSON document cannot be null");
      name = requireNonNull(json.getString("name"), "The product name cannot be null");
      price = new BigDecimal(json.getString("price"));
    } catch (Throwable err) {
      logger.error("Could not extract values", err);
      return Uni.createFrom().failure(err);
    }

    Product product = new Product();
    product.setName(name);
    product.setPrice(price);

    return emf.withSession(session -> session
      .persist(product)
      .chain(session::flush)
      .replaceWith(product));
  }

  private Uni<Product> getProduct(RoutingContext rc) {
    logger.info("getProduct");

    return Uni.createFrom().item(() -> {
      String idParam = rc.pathParam("id");
      return Long.parseLong(idParam);
    })
      .chain(id -> emf.withSession(session -> session.find(Product.class, id)));
  }
}

