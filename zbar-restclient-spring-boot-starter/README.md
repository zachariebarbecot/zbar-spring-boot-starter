# zbar-restclient-spring-boot-starter

Starter Spring Boot qui pose des défauts de production pour les appels HTTP sortants via `RestClient`, sans configuration :

- Timeouts de connexion (2 s) et de lecture (10 s).
- En-tête `User-Agent` égal à `spring.application.name`.
- Propagation en liste blanche des en-têtes de la requête entrante (`X-Correlation-Id`, `X-Request-Id`).
- Exceptions typées pour les réponses 4xx et 5xx, avec le `ProblemDetail` de la réponse.
- Base URL globale optionnelle.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-restclient-spring-boot-starter</artifactId>
</dependency>
```

Le starter apporte `spring-boot-starter-restclient` en transitif.

## Propriétés posées

| Propriété | Valeur | Effet |
|---|---|---|
| `spring.http.clients.connect-timeout` | `2s` | Délai maximal d'établissement de la connexion. |
| `spring.http.clients.read-timeout` | `10s` | Délai maximal d'attente de la réponse. |

## User-Agent

Le `User-Agent` de chaque appel sortant vaut `spring.application.name`. Sans nom défini (ou vide), aucun `User-Agent` n'est posé par le starter.

Un en-tête `User-Agent` posé explicitement sur la requête l'emporte. Pour changer la valeur globalement, déclarer un `RestClientCustomizer` :

```java
@Bean
RestClientCustomizer userAgent() {
  return builder -> builder.defaultHeader(HttpHeaders.USER_AGENT, "mon-agent/1.0");
}
```

## Base URL

```yaml
zbar:
  restclient:
    base-url: https://api.example.com
```

Quand elle est définie et non vide, la base URL s'applique à tous les `RestClient` construits via le `RestClient.Builder` de Spring Boot, pour les URI relatives. Elle est vide par défaut.

## Propagation d'en-têtes

Les en-têtes de la requête entrante listés ci-dessous sont recopiés sur chaque appel sortant :

- `X-Correlation-Id` et `X-Request-Id` par défaut ;
- ceux de `zbar.restclient.propagated-headers`, en plus des défauts (liste additive).

```yaml
zbar:
  restclient:
    propagated-headers:
      - X-Tenant-Id
```

Règles :

- Un en-tête déjà présent sur la requête sortante n'est pas écrasé.
- `Authorization`, `Cookie` et `Proxy-Authorization` ne sont jamais transmis, même s'ils sont listés.
- Sans requête entrante (tâche planifiée, démarrage), rien n'est recopié.

## Erreurs HTTP

Les réponses 4xx lèvent `RestClientClientErrorException`, les réponses 5xx lèvent `RestClientServerErrorException`. Elles remplacent `HttpClientErrorException` et `HttpServerErrorException` de Spring : un `catch` sur ces dernières ne les attrape plus. Les deux héritent de `RestClientStatusException`, elle-même une `RestClientException`.

| Accesseur | Contenu |
|---|---|
| `statusCode()` | Statut HTTP de la réponse. |
| `method()` | Méthode HTTP de l'appel. |
| `url()` | URL complète appelée. |
| `problemDetail()` | `ProblemDetail` de la réponse si elle est de type `application/problem+json` et lisible, sinon `null`. |

```java
try {
  return Optional.ofNullable(client.get().uri("/orders/42").retrieve().body(Order.class));
} catch (RestClientClientErrorException e) {
  if (e.statusCode().value() == 404) {
    return Optional.empty();
  }
  throw e;
}
```

Le message de l'exception est de la forme `404 on GET http://host/orders/42`. Il ne contient ni query, ni userinfo, ni fragment, ni corps de réponse. `url()` contient en revanche la query : ne pas la logguer telle quelle (elle peut porter des jetons ou des données personnelles).

## Surcharge

Les valeurs du starter (timeouts) ont la priorité la plus basse : toute valeur posée par l'application (`application.yml`, variable d'environnement, argument) l'emporte.

```yaml
spring:
  http:
    clients:
      connect-timeout: 500ms
      read-timeout: 30s
```

Pour le `User-Agent`, voir la section User-Agent ci-dessus.

## Désactivation

```yaml
zbar:
  restclient:
    enabled: false
```

`enabled` vaut `true` par défaut. À `false`, le starter ne pose ni timeouts, ni `User-Agent`, ni base URL, ni propagation d'en-têtes, ni exceptions typées : les défauts de Spring Boot s'appliquent.

## Relation avec les autres starters

- **zbar-logs** : les logs des appels sortants et l'en-tête `traceparent` restent actifs. En erreur, la ligne de fin est de la forme `outgoing end GET host:port/path failed=RestClientClientErrorException ...` (ou `RestClientServerErrorException`).
- **zbar-errors** : aucun mapping n'est fourni. Une exception non gérée par l'application sort de l'API en 500 générique ; c'est à l'application de décider quoi renvoyer à son propre appelant.

## Limites

- La propagation d'en-têtes ne fonctionne que dans une application servlet. Hors servlet, le starter ne la configure pas.
- Elle repose sur le contexte de la requête entrante, lié au thread. Dans un thread asynchrone ou un virtual thread lancé par l'application (`@Async`, `CompletableFuture`, executor), le contexte est perdu et aucun en-tête n'est recopié. Lire les en-têtes dans le thread de la requête et les poser explicitement sur l'appel.
