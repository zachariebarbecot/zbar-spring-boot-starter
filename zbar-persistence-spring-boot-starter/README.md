# zbar-persistence-spring-boot-starter

Starter Spring Boot qui pose des défauts JPA/Hibernate propres, sans configuration. Il apporte `spring-boot-starter-data-jpa` et `spring-boot-starter-liquibase` en transitif. Aucun driver JDBC n'est fourni : l'application déclare le sien.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-persistence-spring-boot-starter</artifactId>
</dependency>
```

## Défauts JPA

| Propriété | Valeur |
|---|---|
| `spring.jpa.open-in-view` | `false` |
| `spring.jpa.show-sql` | `false` |
| `spring.jpa.hibernate.ddl-auto` | `validate` si Liquibase actif, sinon `none` |
| `spring.jpa.hibernate.naming.physical-strategy` | `CamelCaseToUnderscoresNamingStrategy` |
| `spring.jpa.hibernate.naming.implicit-strategy` | `SpringImplicitNamingStrategy` |
| `spring.jpa.properties.hibernate.jdbc.time_zone` | `UTC` |
| `spring.jpa.properties.hibernate.jdbc.batch_size` | `50` |
| `spring.jpa.properties.hibernate.order_inserts` | `true` |
| `spring.jpa.properties.hibernate.order_updates` | `true` |
| `spring.jpa.properties.hibernate.default_batch_fetch_size` | `16` |
| `spring.jpa.properties.hibernate.query.fail_on_pagination_over_collection_fetch` | `true` |

Toute valeur définie par l'application (`application.properties`, variable d'environnement, argument de ligne de commande) l'emporte ; les autres défauts restent appliqués.

## Défauts Liquibase

| Propriété | Valeur |
|---|---|
| `spring.liquibase.change-log` | `classpath:db/changelog/db.changelog-master.yaml` |
| `spring.liquibase.drop-first` | `false` |

Liquibase est piloté par `spring.liquibase.enabled` (Boot). Tant qu'il n'est désactivé ni par `spring.liquibase.enabled=false` ni par `zbar.persistence.liquibase.enabled=false`, Hibernate valide le schéma produit par Liquibase (`ddl-auto=validate`) ; une valeur `ddl-auto` explicite l'emporte toujours. Sans changelog à l'emplacement par défaut, le démarrage échoue (comportement Boot).

`@DataJpaTest` remplace la DataSource par H2 et exécute Liquibase : poser `spring.liquibase.enabled=false` (et `spring.jpa.hibernate.ddl-auto=create-drop`) dans un `application-test`, ou `@AutoConfigureTestDatabase(replace = NONE)` avec un PostgreSQL de test.

## Désactivation

| Propriété | Défaut | Effet |
|---|---|---|
| `zbar.persistence.enabled` | `true` | `false` : aucun défaut posé. |
| `zbar.persistence.jpa.enabled` | `true` | `false` : aucun défaut JPA posé. |
| `zbar.persistence.liquibase.enabled` | `true` | `false` : retire les défauts `spring.liquibase.*` et pose `ddl-auto=none`. Ne désactive pas Liquibase (voir `spring.liquibase.enabled`). |

Sans `DataSource`, l'application démarre sans `EntityManagerFactory` ni `SpringLiquibase`.
