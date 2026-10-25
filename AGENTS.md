# AGENTS.md

Instructions pour les agents de code qui travaillent sur ce dépôt.

## Projet

Bibliothèque Maven multi-module de starters Spring Boot `fr.zbar`. Chaque starter pose des défauts de production « sans configuration » pour une application Spring Boot consommatrice.

- Java 25, Spring Boot 4.1.1 (modulaire : `spring-boot-webmvc`, `spring-boot-liquibase`, etc.), Maven ≥ 3.9.9 via `./mvnw`.
- Jackson 3 (`tools.jackson.*`), pas `com.fasterxml.jackson.*`.
- Version unique `0.0.1-SNAPSHOT` pour tous les modules.

## Modules

| Module | Rôle |
|--------|------|
| `zbar-parent` | Parent de tous les starters (hérite de `spring-boot-starter-parent`). Plugins : Spotless, enforcer, failsafe, JaCoCo, flatten, build-info, git-commit-id. Profil `liquibase-diff`. |
| `zbar-bom` | BOM : versions des starters et des dépendances tierces (springdoc, ArchUnit, MockServer, Cucumber…). |
| `zbar-actuator-spring-boot-starter` | Exposition Actuator (`health`, `info`, `prometheus`), virtual threads. |
| `zbar-errors-spring-boot-starter` | Réponses d'erreur RFC 9457 (`application/problem+json`), trace id, intégration OpenAPI. |
| `zbar-logs-spring-boot-starter` | Logs homogènes (console ou JSON `logstash` avec profil `json`), logs d'appels entrants et sortants, propagation de contexte. |
| `zbar-openapi-spring-boot-starter` | Documentation OpenAPI via springdoc. |
| `zbar-persistence-spring-boot-starter` | Défauts JPA/Hibernate et Liquibase. |
| `zbar-restclient-spring-boot-starter` | `RestClient` : timeouts, User-Agent, base-url, propagation d'en-têtes, erreurs HTTP typées. |
| `zbar-test-spring-boot-starter` | Support de test : `@ZbarPostgresTest`, `@ZbarMockServerTest`, `@ZbarSpringBootTest` (Testcontainers), règles ArchUnit. |
| `zbar-transaction-spring-boot-starter` | Bean `Transactions` : transactions programmatiques sans `@Transactional`. |

Nouveau starter : l'ajouter dans `<modules>` du `pom.xml` racine (ordre alphabétique) et dans `zbar-bom/pom.xml`.

## Conventions de code

- Package `fr.zbar.starter.<module>`, une classe par concept, classes package-private sauf l'API publique.
- Deux mécanismes, selon le besoin :
  - **Défauts de propriétés Spring** : `<Module>EnvironmentPostProcessor` (`@Order(Ordered.LOWEST_PRECEDENCE)`), `MapPropertySource` ajoutée en `addLast` pour que l'application garde la main. Déclaré dans `META-INF/spring.factories` sous `org.springframework.boot.EnvironmentPostProcessor`.
  - **Beans** : `<Module>AutoConfiguration` avec `@AutoConfiguration`, déclarée dans `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`. Beans en `@ConditionalOnMissingBean` pour rester surchargeables.
- Chaque starter est désactivable : propriété `zbar.<module>.enabled` (`matchIfMissing = true`).
- Propriétés : `record` `@ConfigurationProperties(prefix = "zbar.<module>")` avec `@DefaultValue`, Javadoc `@param` par champ (alimente les métadonnées via `spring-boot-configuration-processor`).
- Dépendances optionnelles (`<optional>true</optional>`) isolées dans une `@Configuration(proxyBeanMethods = false)` imbriquée avec `@ConditionalOnClass`, pour que le starter charge sans elles.
- Lombok disponible mais peu utilisé : préférer les `record`.
- Formatage imposé par Spotless (google-java-format, imports triés, pas de wildcard, POM trié via sortPom). `spotless:check` tourne en phase `compile` : lancer `./mvnw spotless:apply` avant de commiter.

## Tests

- TDD : test d'abord, puis code. Historique de commits `test:` puis `feat:` / `fix:` par critère.
- `*Test` : surefire (unitaire, sans réseau ni Docker). `*IT` : failsafe (contexte Spring complet, serveur, conteneurs).
- Noms de méthodes : `should_<comportement>_when_<condition>`.
- AssertJ uniquement.
- ITs : démarrent une vraie application via `SpringApplicationBuilder` (`server.port=0`) et appellent l'API avec `RestClient` ; `ApplicationContextRunner` pour les conditions d'autoconfiguration.
- Tester l'absence d'une dépendance optionnelle (`*WithoutXxxTest`) quand le starter doit charger sans elle.
- Les README sont testés quand une doc est un livrable (`PersistenceReadmeTest`) : modifier le README peut casser un test.
- Docker requis pour les ITs Testcontainers (PostgreSQL, MockServer).

### Tests instables connus

Plusieurs ITs HTTP échouent parfois (`HTTP/1.1 header parser received no bytes`, 415 inattendu, trace id) et passent au relancement : `ErrorsResponseIT`, `ErrorsTraceIdIT`, `ErrorsServerErrorIT`, `OpenApiVersionIT`, `LogsRequestLoggingIT`. Idem `ZbarSharedContainersTest` / `ZbarReuseTest` (« Broken pipe » Docker). Relancer avant de chercher une régression ; ne pas masquer avec des retries dans le code.

## Build

```bash
./mvnw verify
```

```bash
./mvnw -pl zbar-errors-spring-boot-starter -am verify
```

- `.mvn/maven.config` impose `-B -ntp`.
- Le Maven build cache (`.mvn/extensions.xml`) peut laisser des rapports surefire périmés : pour une vérification finale, ajouter `-Dmaven.build.cache.skipCache=true` ou `clean`.
- `git-commit-id` lit `.git` : ne pas builder hors d'un dépôt git si les tests `info` doivent passer.

### Profil `liquibase-diff`

Outil pour les applications consommatrices : génère un changelog de diff entre les entités JPA et une base migrée (`-Dliquibase.url/username/password`). Toujours `compile liquibase:diff`, jamais `liquibase:diff` seul : sans phase, la vérification de `zbar.liquibase.entity-package` ne tourne pas. Workflow complet dans le README persistence.

Sortie dans `target/liquibase/diff-changelog.yaml`. Faux positif connu (liquibase-hibernate7) : `addDefaultValue nextval(...)` sur les ids `@GeneratedValue` SEQUENCE ; documenté dans le README persistence.

## Pièges

- Spring Boot 4 est modulaire : une lib seule n'active pas l'autoconfig. Ex. `liquibase-core` seul ne suffit pas, dépendre de `spring-boot-starter-liquibase`.
- Garder `spring-boot.version` de `zbar-parent` et `zbar-bom` alignée sur la version de `spring-boot-starter-parent`.
- Image MockServer : même version mineure que `mockserver-client-java`.

## Documentation

- Un `README.md` par starter, en français : installation, comportement par défaut, tableau des propriétés, désactivation. Le mettre à jour avec chaque changement de comportement ou de propriété.
- Code, Javadoc, noms de tests et messages de commit en anglais.

## Git et workflow

- Branche `main`. Commits Conventional Commits en anglais (`feat:`, `fix:`, `test:`, `docs:`, `refactor:`, `chore:`).
- PR titrées en français, format `Starter <Nom> : <résumé>`, fusionnées en squash.
- Workflow Gobanos : cadrages dans `.gobanos/cadrage-<slug>.md` (jamais commités), une story par branche `gobanos/<slug>/us-NN`, worktrees dans `.worktrees/`, PR empilées sur la branche parente.
- `.projectmem/` : mémoire projet, écrite via les outils MCP projectmem, jamais à la main.
- Ne jamais se créditer comme co-auteur dans les commits ou PR.
