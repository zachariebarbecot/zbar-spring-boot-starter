# zbar-errors-spring-boot-starter

Starter Spring Boot pour des réponses d'erreur uniformes au format RFC 9457 (`application/problem+json`), sans configuration.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-errors-spring-boot-starter</artifactId>
</dependency>
```

## Propriétés

| Propriété | Défaut | Description |
|---|---|---|
| `zbar.errors.enabled` | `true` | Active le starter ; à `false`, aucun bean du starter n'est chargé. |

## Format de réponse

Toute erreur Spring MVC est renvoyée en `application/problem+json`, y compris quand le client envoie `Accept: application/json` :

```json
{
  "title": "Not Found",
  "status": 404,
  "detail": "No static resource unknown.",
  "instance": "/unknown"
}
```

- `type` vaut `about:blank` (champ omis, valeur par défaut de la RFC 9457) ;
- `title` est la phrase HTTP du statut ;
- `detail` est générique, en anglais, sans message Jackson, nom de classe ni valeur envoyée.

Erreurs prises en charge : `ResponseStatusException`, 404, 405 (avec l'en-tête `Allow`), 415, et 400 (JSON illisible, paramètre obligatoire manquant).

## Erreurs 500

Toute exception non gérée, et toute `ResponseStatusException` 5xx, produit un `application/problem+json` au `detail` générique :

```json
{
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred.",
  "instance": "/boom"
}
```

- le message d'exception, la stacktrace et le nom de classe ne sont jamais exposés, quels que soient `server.error.include-stacktrace`, `include-message` et `include-exception` ;
- exactement un log `ERROR` avec la stacktrace est émis par erreur 5xx ;
- les erreurs 4xx gérées n'émettent aucun log `ERROR`.

## traceId

Quand un span valide existe (Micrometer Tracing), le corps de toute erreur 4xx ou 5xx contient `traceId`, celui du span courant : le `traceparent` reçu s'il y en a un, le même identifiant que dans les logs du starter logs.

```json
{
  "title": "Internal Server Error",
  "status": 500,
  "detail": "An unexpected error occurred.",
  "instance": "/boom",
  "traceId": "0af7651916cd43dd8448eb211c80319c"
}
```

- sans span valide, ou sans Micrometer Tracing ni bean `Tracer`, `traceId` est absent (jamais `null` ni vide) et la réponse reste valide ;
- `micrometer-tracing` est optionnel : ajoutez-le (par exemple via un bridge OpenTelemetry) pour activer `traceId` ; aucun identifiant de secours n'est généré.

## Validation

Une erreur de validation renvoie 400 en `application/problem+json` avec `errors`, une entrée `{field, message}` par violation, triée par `field` :

```json
{
  "title": "Bad Request",
  "status": 400,
  "detail": "Invalid request content.",
  "instance": "/signup",
  "errors": [
    {"field": "password", "message": "size must be between 8 and 2147483647"},
    {"field": "username", "message": "must not be blank"}
  ]
}
```

- cas couverts : body `@Valid`, paramètre de requête ou de chemin contraint (`@Validated` ou validation de méthode Spring), type de paramètre incorrect ;
- une violation globale (sans champ) utilise le nom de l'objet comme `field`, jamais `null` ;
- la valeur rejetée n'apparaît jamais, ni dans `errors` ni dans `detail` ;
- le `message` est celui de la contrainte (langue négociée par Spring) ; pas de code d'erreur par contrainte ;
- `jakarta.validation` est optionnel : sans lui, seules les validations Spring MVC sont détaillées.

## @ResponseStatus et Spring Security

Une exception annotée `@ResponseStatus` (directement ou en méta-annotation) garde son statut : le starter ne la transforme pas en 500, et n'émet aucun log `ERROR` pour un 4xx.

Si Spring Security est présent, `AccessDeniedException` (y compris `AuthorizationDeniedException`) et `AuthenticationException` ne sont pas gérées par le starter : Spring Security les traduit en 403 et 401, comme sans le starter.

## Schéma OpenAPI

Quand springdoc (`springdoc-openapi-starter-webmvc-ui`) est présent, le starter enrichit `/v3/api-docs` :

- `components.schemas.ProblemDetail` décrit `type`, `title`, `status`, `detail`, `instance`, `traceId` et `errors[{field, message}]` ;
- chaque opération reçoit des réponses 400, 404 et 500 en `application/problem+json` qui référencent `ProblemDetail` ;
- une réponse 400, 404 ou 500 déjà déclarée par l'application (par exemple `@ApiResponse`) est conservée telle quelle ;
- springdoc est optionnel : sans lui, le starter démarre normalement et les erreurs restent correctes.

Ce comportement est indépendant de celui de `zbar-openapi-spring-boot-starter`.

## Cohabitation

- Un `@RestControllerAdvice` de l'application étendant `ResponseEntityExceptionHandler` remplace celui du starter ; un advice qui ne l'étend pas reste prioritaire sur les exceptions qu'il gère.
- `spring.mvc.problemdetails.enabled=true` est compatible : une seule réponse `application/problem+json`, sans conflit de beans.

## Limites

- les réponses 401/403 de Spring Security ne sont pas au format `application/problem+json` et ne sont pas ajoutées à l'OpenAPI ;
- les erreurs hors MVC (page `/error` du conteneur, exceptions levées dans des filtres) ne passent pas par le starter ;
- hors périmètre : WebFlux, codes d'erreur métier, i18n.
