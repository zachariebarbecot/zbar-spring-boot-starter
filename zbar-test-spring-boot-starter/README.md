# zbar-test-spring-boot-starter

Starter de test qui fournit des conteneurs Testcontainers pré-câblés, sans configuration :

- `@ZbarPostgresTest` : une base PostgreSQL démarrée et branchée sur la `DataSource`.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`). Le starter se déclare en scope `test`.

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-test-spring-boot-starter</artifactId>
  <scope>test</scope>
</dependency>
```

Le starter apporte `spring-boot-testcontainers`, `testcontainers-postgresql` et `testcontainers-junit-jupiter` en transitif. Docker doit tourner sur le poste.

## `@ZbarPostgresTest`

Annotation composée : `@SpringBootTest` + démarrage d'un conteneur PostgreSQL branché par `@ServiceConnection`. Aucune propriété `spring.datasource.*` n'est nécessaire.

```java
@ZbarPostgresTest
class OrderRepositoryIT {

  @Autowired JdbcTemplate jdbc;

  @Test
  void should_query_postgres() {
    assertThat(jdbc.queryForObject("select 1", Integer.class)).isEqualTo(1);
  }
}
```

Le pilote JDBC PostgreSQL reste à déclarer par l'application.

## Propriétés

| Propriété | Défaut | Effet |
|---|---|---|
| `zbar.test.postgres.image` | `postgres:17.6` | Image Docker utilisée pour le conteneur PostgreSQL. |

```java
@ZbarPostgresTest
@TestPropertySource(properties = "zbar.test.postgres.image=postgres:18-alpine")
class OrderRepositoryIT {}
```
