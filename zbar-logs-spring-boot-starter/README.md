# zbar-logs-spring-boot-starter

Starter Spring Boot pour des logs homogènes, sans configuration :

- console lisible par défaut, JSON `logstash` avec le profil `json` ;
- corrélation de bout en bout par OpenTelemetry (W3C `traceparent`) : `traceId` et `spanId` dans chaque log ;
- logs de début et de fin (statut, durée) des appels REST entrants et sortants ;
- niveaux par package via `logging.level.*` natif de Spring Boot.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-logs-spring-boot-starter</artifactId>
</dependency>
```

## Propriétés

| Propriété | Défaut | Description |
|---|---|---|
| `zbar.logs.enabled` | `true` | Active le starter. |
| `zbar.logs.exclude-paths` | `/actuator/**`, `/v3/api-docs` | Chemins sans logs début/fin ; la liste remplace le défaut. |

Exemple : ne plus logger que les appels hors `/internal/**` (les défauts `/actuator/**` et `/v3/api-docs` sont alors loggés).

```yaml
zbar:
  logs:
    exclude-paths:
      - /internal/**
```

## Format des logs : console et JSON

Par défaut, la sortie est la console Spring Boot, avec `traceId-spanId` dans la ligne :

```
2026-10-24T10:15:30.123+02:00  INFO 4242 --- [demo] [nio-8080-exec-1] [0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331] f.z.demo.HelloController : hello
```

Le profil `json` passe la console au format JSON `logstash` natif de Spring Boot : un objet JSON par ligne, stack trace comprise.

```
SPRING_PROFILES_ACTIVE=json java -jar app.jar
```

```json
{"@timestamp":"2026-10-24T10:15:30.123+02:00","@version":"1","message":"hello","logger_name":"fr.zbar.demo.HelloController","thread_name":"http-nio-8080-exec-1","level":"INFO","level_value":20000,"traceId":"0af7651916cd43dd8448eb211c80319c","spanId":"b7ad6b7169203331"}
```

Le format posé par l'application (`logging.structured.format.console`, par exemple `ecs`) reste prioritaire sur le profil `json`. Avec `zbar.logs.enabled=false`, le profil `json` est sans effet.

## Corrélation et traceId

Le starter apporte Micrometer Tracing avec le bridge OpenTelemetry. Chaque requête entrante porte un `traceId` :

- repris de l'en-tête `traceparent` (W3C Trace Context) s'il est valide ;
- créé sinon (`traceparent` absent ou invalide).

Le `traceId` et le `spanId` figurent dans tous les logs de la requête. Le starter n'ajoute aucun en-tête de réponse.

Côté front, envoyer un `traceparent` pour rattacher les logs à l'action de l'utilisateur :

```
traceparent: 00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01
```

Format : `00-<traceId 32 hex>-<spanId 16 hex>-<flags 2 hex>`.

Le sampling vaut `1.0` par défaut (`management.tracing.sampling.probability`), pour que chaque `traceparent` sortant soit échantillonné. L'application peut le modifier.

### Exécution asynchrone

Le starter pose `spring.task.execution.propagate-context=true` par défaut : les logs d'une méthode `@Async` ou `@Scheduled` portent le `traceId` de leur origine. Un `TaskDecorator` applicatif reste appliqué en plus de la propagation. L'application peut désactiver la propagation avec `spring.task.execution.propagate-context=false`.

## Logs début/fin

Niveau INFO, un log au début et un à la fin de chaque appel. Seuls la méthode et le chemin sont loggés : jamais la query string, les en-têtes ni les corps.

Appels entrants (hors `zbar.logs.exclude-paths`) :

```
request start GET /orders
request end GET /orders status=200 duration=12ms
```

Appels sortants (`RestClient` et `RestTemplate`), avec hôte et chemin de la cible :

```
outgoing start GET shop.example.com/items
outgoing end GET shop.example.com/items status=200 duration=35ms
outgoing end GET shop.example.com/items failed=ResourceAccessException duration=5ms
```

`failed=<Type>` remplace `status=` quand l'appel échoue sans réponse (timeout, connexion refusée). Les appels sortants portent le `traceparent` vers le service aval.

Le starter n'apporte pas `spring-boot-starter-restclient` : l'application le déclare pour disposer d'un builder instrumenté.

Pour être instrumenté, un client doit être créé à partir du builder fourni par Spring Boot (`RestClient.Builder`, `RestTemplateBuilder`).

## Niveaux de log par package

Les niveaux utilisent le mécanisme natif de Spring Boot, en console comme en JSON :

```yaml
logging:
  level:
    com.acme: WARN
    fr.zbar.starter.logs: DEBUG
```

Avec `logging.level.com.acme=WARN`, les logs INFO de `com.acme` sont absents.

## Désactivation

```yaml
zbar:
  logs:
    enabled: false
```

`enabled` vaut `true` par défaut. À `false`, le starter ne crée aucun bean (plus de logs début/fin) et le profil `json` n'a plus d'effet.

Avec `zbar.logs.enabled=false`, le sampling n'est plus forcé à `1.0` et revient au défaut de Spring Boot (`0.1`).

`zbar.logs.enabled=false` ne coupe pas le tracing de Spring Boot. Pour le couper aussi :

```yaml
management:
  tracing:
    enabled: false
```

## Limites

- Servlet / Spring WebMVC seulement ; WebFlux n'est pas supporté.
- Les clients créés avec `new` (`new RestTemplate()`, `RestClient.create()`) ne sont pas instrumentés : ni logs sortants ni propagation du `traceparent`.
- La propagation du contexte ne couvre que les executors configurés par Spring Boot : les executors créés à la main (`new`, `Executors.*`) et les `CompletableFuture` sans executor Spring perdent le `traceId`.
- Pas d'export OTLP : le tracing sert à la corrélation des logs, pas à l'envoi de traces vers un collecteur.
- Pas de masquage ni de log des corps de requête et de réponse.
