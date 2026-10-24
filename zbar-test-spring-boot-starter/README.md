# zbar-test-spring-boot-starter

Starter de test qui fournit des conteneurs Testcontainers pré-câblés, sans configuration :

- `@ZbarPostgresTest` : une base PostgreSQL démarrée et branchée sur la `DataSource`.
- `@ZbarMockServerTest` : un MockServer démarré, son URL publiée dans l'`Environment` et un `MockServerClient` injectable.
- `@ZbarSpringBootTest` : les deux à la fois, partagés dans la JVM.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`). Le starter se déclare en scope `test`.

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-test-spring-boot-starter</artifactId>
  <scope>test</scope>
</dependency>
```

Le starter apporte `spring-boot-testcontainers`, `testcontainers-postgresql`, `testcontainers-mockserver`, `mockserver-client-java`, `testcontainers-junit-jupiter` et `archunit-junit5` en transitif. Docker doit tourner sur le poste.

Déclaré en scope `test`, le starter n'ajoute ni Testcontainers, ni MockServer, ni ArchUnit, ni `org.springframework` aux classpaths `compile` et `runtime` du module (vérifié à chaque build par un projet témoin).

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

## `@ZbarMockServerTest`

Annotation composée : `@SpringBootTest` + démarrage d'un conteneur MockServer. Les propriétés `zbar.test.mockserver.url`, `zbar.test.mockserver.host` et `zbar.test.mockserver.port` sont résolues dans l'`Environment`. Un bean `MockServerClient` est injectable pour définir les attentes ; il est réinitialisé avant chaque test. Si le test déclare son propre bean `MockServerClient`, celui du starter n'est pas créé.

```java
@ZbarMockServerTest
class PaymentClientIT {

  @Autowired MockServerClient mockServer;

  @Value("${zbar.test.mockserver.url}")
  String url;

  @Test
  void should_call_stubbed_api() {
    mockServer.when(request().withPath("/x")).respond(response().withBody("hi"));

    assertThat(RestClient.create(url).get().uri("/x").retrieve().body(String.class)).isEqualTo("hi");
  }
}
```

Pour brancher l'URL sur la propriété d'un adapter, la mapper dans `application-test.yml` :

```yaml
payment:
  base-url: ${zbar.test.mockserver.url}
```

## `@ZbarSpringBootTest`

Annotation composée : `@SpringBootTest` + PostgreSQL + MockServer, avec le reset des attentes MockServer avant chaque test. Un seul conteneur de chaque type est démarré par JVM (par image), quel que soit le nombre de contextes Spring différents. Si une configuration de test déclare son propre `PostgreSQLContainer` ou `JdbcConnectionDetails` (resp. `MockServerContainer`), le starter ne crée pas le sien. Sans Docker, le test échoue avec un message indiquant que Docker est requis.

```java
@ZbarSpringBootTest
class ApplicationIT {}
```

### Réutilisation des conteneurs

`zbar.test.reuse=true` applique `withReuse(true)` aux conteneurs (désactivé par défaut). Testcontainers ne réutilise réellement un conteneur d'une exécution à l'autre que si le poste l'autorise aussi : `testcontainers.reuse.enable=true` dans `~/.testcontainers.properties`.

## Propriétés

| Propriété | Défaut | Effet |
|---|---|---|
| `zbar.test.postgres.image` | `postgres:17.6` | Image Docker utilisée pour le conteneur PostgreSQL. |
| `zbar.test.mockserver.image` | `mockserver/mockserver:5.15.0` | Image Docker utilisée pour le conteneur MockServer (garder la même version mineure que `mockserver-client-java`). |
| `zbar.test.mockserver.url` / `.host` / `.port` | publiées | Adresse du MockServer démarré (lecture seule). |
| `zbar.test.reuse` | `false` | Applique `withReuse(true)` aux conteneurs PostgreSQL et MockServer. |

```java
@ZbarPostgresTest
@TestPropertySource(properties = "zbar.test.postgres.image=postgres:18-alpine")
class OrderRepositoryIT {}
```

## Règles ArchUnit

`ZbarArchitectureRules` garantit qu'un domaine ne dépend ni de Spring ni des adapters. Test ArchUnit pur : ni contexte Spring ni Docker.

- Les classes `..domain..` ne dépendent pas de `org.springframework..`.
- Les classes `..domain..` ne dépendent pas des classes `..adapter..`.

```java
class ArchitectureTest {

  private final JavaClasses classes = new ClassFileImporter().importPackages("com.example.shop");

  @Test
  void should_keep_domain_free_of_spring_and_adapters() {
    new ZbarArchitectureRules().check(classes);
  }
}
```

En cas de violation, un `AssertionError` cite la classe fautive et la dépendance interdite. Les packages se paramètrent :

```java
new ZbarArchitectureRules("..core..", "..infra..").check(classes);
```
