## Demo

### Running Postgres

```
docker run --name some-postgres --rm -e POSTGRES_DB=postgres -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=vertx-in-action -p 5432:5432 postgres
```


### Testing

```
curl -X POST -H "Content-Type: application/json" -d '{"id":"spoon","name":"Spoon","price":1.0}' http://localhost:8080/products

curl http://localhost:8080/products

curl http://localhost:8080/products/1
```
