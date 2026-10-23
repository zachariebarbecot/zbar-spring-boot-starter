# zbar-openapi-spring-boot-starter

Starter Spring Boot qui expose la documentation OpenAPI de l'application via [springdoc](https://springdoc.org/), sans configuration.

- `GET /v3/api-docs` : document OpenAPI.
- `GET /swagger-ui/index.html` : Swagger UI.
- Titre de la documentation = `spring.application.name`.
- Version de la documentation = commit git abrégé (`git.commit.id.abbrev`).

## Installation

La version du starter et celle de springdoc sont gérées par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-openapi-spring-boot-starter</artifactId>
</dependency>
```

Le starter apporte `springdoc-openapi-starter-webmvc-ui` en transitif : il vise une application Spring MVC. WebFlux n'est pas supporté.

## Titre et version

| Champ OpenAPI | Source | Si la source est absente |
|---|---|---|
| `info.title` | `spring.application.name` | défaut springdoc `OpenAPI definition` |
| `info.version` | `git.commit.id.abbrev` de `git.properties` | défaut springdoc `v0` |

`git.properties` est produit au build par le `git-commit-id-maven-plugin` configuré dans `zbar-parent`. Spring Boot le charge en bean `GitProperties`.

Un bean `OpenAPI` défini par l'application est conservé. Le starter ne remplit le titre et la version que s'ils sont vides ou égaux aux défauts springdoc.

## Désactivation

```yaml
zbar:
  openapi:
    enabled: false
```

`enabled` vaut `true` par défaut. À `false`, le starter ne crée aucun bean et coupe aussi springdoc (`springdoc.api-docs.enabled=false`, `springdoc.swagger-ui.enabled=false`) : `/v3/api-docs` et Swagger UI répondent 404.

Une valeur springdoc posée explicitement par l'application (par exemple `springdoc.api-docs.enabled: true`) reste prioritaire.
