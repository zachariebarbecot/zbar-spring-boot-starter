# AGENTS.md

Library of Spring Boot starters (`fr.zbar`). Each starter applies production defaults to a consuming application with zero configuration.

## Stack

- Java 25, Spring Boot 4.1.1 (modular: `spring-boot-webmvc`, `spring-boot-liquibase`…), Jackson 3 (`tools.jackson.*`).
- Maven multi-module via `./mvnw`, single version `0.0.1-SNAPSHOT`. Build cache extension enabled.
- Tests: JUnit 5, AssertJ, Testcontainers (PostgreSQL, MockServer), ArchUnit.
- Repository on GitHub (`origin`); PRs opened with `gh pr create`.

## Modules

- `zbar-parent`: parent of every starter (Spotless, enforcer, failsafe, JaCoCo, flatten, `liquibase-diff` profile).
- `zbar-bom`: versions of the starters and third-party libraries. A new starter goes into the root `<modules>` and into the BOM.
- `zbar-actuator-`, `zbar-errors-`, `zbar-logs-`, `zbar-openapi-`, `zbar-persistence-`, `zbar-restclient-`, `zbar-transaction-spring-boot-starter`: runtime starters.
- `zbar-test-spring-boot-starter`: test support (`@ZbarPostgresTest`, `@ZbarMockServerTest`, `@ZbarSpringBootTest`, `ZbarArchitectureRules`).

## Architecture

A library, not an application: no hexagonal layers, no domain. One flat package `fr.zbar.starter.<module>` per starter; only the public API is `public`.

- Property defaults: `<Module>EnvironmentPostProcessor`, `@Order(LOWEST_PRECEDENCE)`, `MapPropertySource` added with `addLast` so the application wins. Registered in `META-INF/spring.factories`.
- Beans: `<Module>AutoConfiguration`, registered in `META-INF/spring/...AutoConfiguration.imports`, every bean `@ConditionalOnMissingBean`.
- Each starter can be turned off with `zbar.<module>.enabled` (`matchIfMissing = true`).
- Properties: `record` `@ConfigurationProperties(prefix = "zbar.<module>")` with `@DefaultValue` and one Javadoc `@param` per field.
- Optional dependency (`<optional>true</optional>`): isolate it in a nested `@Configuration(proxyBeanMethods = false)` with `@ConditionalOnClass`, and cover the starter loading without it (`*WithoutXxxTest`).

## Tests

- `*Test` = surefire (no network, no Docker), `*IT` = failsafe.
- ITs start a real application with `SpringApplicationBuilder` (`server.port=0`) and call it with `RestClient`; `ApplicationContextRunner` for auto-configuration conditions.
- When a README is a deliverable, a test asserts its content (`PersistenceReadmeTest`): editing the README can break it.
- Known flaky ITs, green on rerun: `ErrorsResponseIT`, `ErrorsTraceIdIT`, `ErrorsServerErrorIT`, `OpenApiVersionIT`, `LogsRequestLoggingIT` (HTTP "header parser received no bytes"), `ZbarSharedContainersTest`, `ZbarReuseTest` (Docker "Broken pipe"). Rerun before looking for a regression.

## Pitfalls

- Spring Boot 4 is modular: a library alone does not trigger its auto-configuration (`liquibase-core` alone does not; use `spring-boot-starter-liquibase`).
- Keep `spring-boot.version` in `zbar-parent` and `zbar-bom` equal to the `spring-boot-starter-parent` version.
- MockServer image: same minor version as `mockserver-client-java`.
- `liquibase-diff` profile: always `compile liquibase:diff`, never `liquibase:diff` alone. Known false positive on `@GeneratedValue` SEQUENCE ids (`addDefaultValue nextval`), documented in the persistence README.

## Documentation

One `README.md` per starter, written in French: installation, default behaviour, property table, how to disable. Update it with every behaviour or property change.

## CI and release

- `.github/workflows/ci.yml`: `./mvnw install` on push to `main` and on pull requests, with test reruns for the known flaky ITs. Branch and tag protection: see the "Rulesets" section.
- `.github/workflows/release.yml`: runs on a pushed tag `vX.Y.Z`. Order: tag format check, tag is an ancestor of `origin/main`, `gh release view` fails if the Release exists, `versions:set` (not committed), `install` (so the nested `mvnw` of `LiquibaseDiffProfileIT` finds the modules in `~/.m2`), `deploy` to GitHub Packages, `gh release create --generate-notes` with jars and poms. Only `GITHUB_TOKEN` is used (`contents: write`, `packages: write`). The `-SNAPSHOT` version stays in the repository; the tag sets the release version at build time.
- `distributionManagement` (id `github`) lives in `zbar-parent` and `zbar-bom`.
- Partial deploy (job fails after some modules are published): GitHub Packages refuses to overwrite a version. Delete the published package versions in the GitHub Packages UI (or with `gh api -X DELETE /user/packages/maven/<package>/versions/<id>`), delete the tag (`git push --delete origin vX.Y.Z`; needs the Admin role, which bypasses the `tags` ruleset), then tag again on `main` and push. No automatic resume.
