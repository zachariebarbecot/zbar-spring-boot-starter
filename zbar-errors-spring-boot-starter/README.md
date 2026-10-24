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

## Cohabitation

- Un `@RestControllerAdvice` de l'application étendant `ResponseEntityExceptionHandler` remplace celui du starter ; un advice qui ne l'étend pas reste prioritaire sur les exceptions qu'il gère.
- `spring.mvc.problemdetails.enabled=true` est compatible : une seule réponse `application/problem+json`, sans conflit de beans.

## Limites

Hors périmètre : 401/403 Spring Security, erreurs hors MVC (`/error` du conteneur, exceptions de filtres), WebFlux, codes d'erreur métier, i18n.
