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

## Générer le changelog avec `liquibase diff`

Le code fait foi : le changelog YAML est généré depuis les entités JPA par le profil Maven `liquibase-diff` de `zbar-parent` (`liquibase-maven-plugin` + `liquibase-hibernate7`, versions alignées sur `liquibase-core` géré par Spring Boot). Le profil n'ajoute aucune exécution au build normal.

Workflow :

1. Créer ou modifier les entités JPA.
2. Migrer une base PostgreSQL locale à l'état courant du master (vierge au premier changelog) avec `mvn -Pliquibase-diff <mêmes -D> compile liquibase:update`, puis lancer le diff contre elle :
   ```
   mvn -Pliquibase-diff -Dzbar.liquibase.entity-package=com.acme.entity \
     -Dliquibase.url=jdbc:postgresql://localhost:5432/app \
     -Dliquibase.username=app -Dliquibase.password=secret \
     compile liquibase:diff
   ```
   `compile` est obligatoire : il compile les entités et déclenche la vérification de `zbar.liquibase.entity-package`. Sans phase du cycle de vie (`liquibase:diff` seul), cette vérification ne tourne pas et la propriété reste non résolue.
   Le YAML est écrit dans `target/liquibase/diff-changelog.yaml`.
3. Relire le YAML généré (types, contraintes, ordre) et le copier sous `src/main/resources/db/changelog/`.
4. L'inclure dans `db.changelog-master.yaml` (`include: file: ...`).

| Propriété Maven | Défaut | Rôle |
|---|---|---|
| `zbar.liquibase.entity-package` | aucun, obligatoire | Package des entités ; absent, le build échoue avec un message explicite. |
| `zbar.liquibase.dialect` | `org.hibernate.dialect.PostgreSQLDialect` | Dialecte Hibernate de la référence. |

La référence est `hibernate:spring:<package>?dialect=...` avec les stratégies de nommage `CamelCaseToUnderscoresNamingStrategy` et `SpringImplicitNamingStrategy`, identiques aux défauts JPA du starter.

Checklist manuelle de reproductibilité (le diff n'est pas exécuté automatiquement) :

- [ ] Deux diffs sur base vierge produisent un YAML identique (hors `id` des changeSets).
- [ ] Après `compile liquibase:update` du master incluant le YAML, un nouveau diff est vide (diff post-migration vide).
- [ ] Faux positif connu (liquibase-hibernate7 5.0.3 et 5.0.4, ids `@GeneratedValue` sur séquence PostgreSQL) : le diff post-migration propose deux changeSets `addDefaultValue` `nextval('<table>_seq'::regclass)` sur la colonne `id`, alors que le default existe déjà en base. Supprimer ces changeSets à la relecture ; tout autre changeSet est un vrai écart. Piste écartée : `IDENTITY` supprime le faux positif mais désactive le batch d'inserts (`hibernate.jdbc.batch_size`).
- [ ] Revoir chaque paire drop/add : elle peut masquer un renommage de colonne ou de table, à réécrire en `renameColumn`/`renameTable` pour ne pas perdre de données.

## Désactivation

| Propriété | Défaut | Effet |
|---|---|---|
| `zbar.persistence.enabled` | `true` | `false` : aucun défaut posé. |
| `zbar.persistence.jpa.enabled` | `true` | `false` : aucun défaut JPA posé. |
| `zbar.persistence.liquibase.enabled` | `true` | `false` : retire les défauts `spring.liquibase.*` et pose `ddl-auto=none`. Ne désactive pas Liquibase (voir `spring.liquibase.enabled`). |

Sans `DataSource`, l'application démarre sans `EntityManagerFactory` ni `SpringLiquibase`.
