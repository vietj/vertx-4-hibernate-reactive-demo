import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import data.Product;
import io.smallrye.mutiny.Uni;
import io.smallrye.mutiny.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.mutiny.ext.web.Router;
import io.vertx.mutiny.ext.web.RoutingContext;
import io.vertx.mutiny.ext.web.handler.BodyHandler;
import org.hibernate.reactive.mutiny.Mutiny;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.Persistence;
import java.math.BigDecimal;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class ApiVerticle extends AbstractVerticle {

  private static final Logger logger = LoggerFactory.getLogger(ApiVerticle.class);

  private Mutiny.SessionFactory emf = Persistence.createEntityManagerFactory("pg-demo").unwrap(Mutiny.SessionFactory.class);
  private ObjectMapper objectMapper = new JsonMapper();

  @Override
  public Uni<Void> asyncStart() {
    Router router = Router.router(vertx);

    BodyHandler bodyHandler = BodyHandler.create();
    router.post().handler(bodyHandler::handle);
    router.get("/products").handler(this::listProducts);
    router.get("/products/:id").handler(this::fetchProduct);
    router.post("/products").handler(this::appendProduct);

    return vertx.createHttpServer()
      .requestHandler(router::handle)
      .listen(8080)
      .onItem().invoke(() -> logger.info("HTTP server listening on port 8080"))
      .replaceWithVoid();
  }

  private <T> void dispatch(String operation, RoutingContext rc, Function<Mutiny.Session, Uni<T>> block) {
    emf.withSession(block).subscribe().with(
      ok -> logger.info("Served {} request from {}", operation, rc.request().remoteAddress()),
      err -> {
        logger.error("Failed to serve {} request from {}", operation, rc.request().remoteAddress(), err);
        rc.fail(500);
      }
    );
  }

  private void listProducts(RoutingContext rc) {
    dispatch("listProducts", rc, session -> session.createQuery("from Product", Product.class)
      .getResultList()
      .chain(this::mapProductsToJson)
      .chain(json -> forwardJson(rc, json)));
  }

  private void fetchProduct(RoutingContext rc) {
    String id = rc.pathParam("id");
    dispatch("fetchProduct", rc, session -> session.find(Product.class, Long.valueOf(id))
      .chain(product -> forwardProductToJson(rc, product)));
  }

  private void appendProduct(RoutingContext rc) {
    JsonObject json = rc.getBodyAsJson();
    String name;
    BigDecimal price;

    try {
      requireNonNull(json, "The incoming JSON document cannot be null");
      name = requireNonNull(json.getString("name"), "The product name cannot be null");
      price = new BigDecimal(json.getString("price"));
    } catch (Throwable err) {
      logger.error("Could not extract values", err);
      rc.fail(400);
      return;
    }

    dispatch("appendProduct", rc, session -> {
      Product product = new Product();
      product.setName(name);
      product.setPrice(price);
      return session
        .persist(product)
        .chain(session::flush)
        .chain(done -> rc.response().setStatusCode(201).end());
    });
  }

  private Uni<Void> forwardProductToJson(RoutingContext rc, Product product) {
    if (product != null) {
      JsonObject json = new JsonObject()
        .put("id", product.getId())
        .put("name", product.getName())
        .put("price", product.getPrice().toString());
      return forwardJson(rc, json.encode());
    } else {
      return rc.response().setStatusCode(404).end();
    }
  }

  private Uni<Void> forwardJson(RoutingContext rc, String json) {
    return rc.response()
      .putHeader("Content-Type", "application/json")
      .end(json);
  }

  private Uni<String> mapProductsToJson(List<Product> products) {
    try {
      return Uni.createFrom().item(objectMapper.writeValueAsString(products));
    } catch (JsonProcessingException mappingError) {
      return Uni.createFrom().failure(mappingError);
    }
  }
}
