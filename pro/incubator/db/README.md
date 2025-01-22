

## Starting PostgreSQL for testing

```shell
docker run --name http4k-test-postgres -p 5432:5432 -e POSTGRES_PASSWORD=mysecretpassword -d postgres:17.2
```

## Starting MySQL for testing

```shell
docker run --name http4k-test-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=mysecretpassword -d mysql:9.2
```
