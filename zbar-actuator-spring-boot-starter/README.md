# zbar-actuator-spring-boot-starter

Starter Spring Boot qui pose des défauts de production pour Actuator, sans configuration :

- `GET /actuator/health`, `GET /actuator/info` et `GET /actuator/prometheus` exposés, et eux seuls.
- Informations git (branche, commit) dans `/actuator/info`.
- Tag `application` sur toutes les métriques.
- Arrêt gracieux avec un timeout de 30 s.
- Virtual threads activés.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-actuator-spring-boot-starter</artifactId>
</dependency>
```

Le starter apporte `spring-boot-starter-actuator` et `micrometer-registry-prometheus` en transitif.

## Propriétés posées

| Propriété | Valeur | Effet |
|---|---|---|
| `management.endpoints.web.exposure.include` | `health,info,prometheus` | Seuls ces endpoints sont exposés en HTTP ; les autres (`env`, `beans`, `metrics`…) répondent 404. |
| `management.metrics.tags.application` | `spring.application.name` | Tag `application` sur toutes les métriques. |
| `server.shutdown` | `graceful` | Les requêtes en cours se terminent avant l'arrêt. |
| `spring.lifecycle.timeout-per-shutdown-phase` | `30s` | Durée maximale de l'arrêt gracieux. |
| `spring.threads.virtual.enabled` | `true` | Les requêtes sont traitées sur des virtual threads. |

Le tag `application` n'est posé que si `spring.application.name` est défini et non vide. Sans nom, aucune série ne porte ce tag.

## Informations git

`git.properties` est produit au build par le `git-commit-id-maven-plugin` configuré dans `zbar-parent`. Spring Boot l'expose dans `/actuator/info` en mode `simple` (branche, commit id, date). Sans `git.properties`, l'application démarre et `/actuator/info` répond sans bloc `git`.

Pour exposer tout le contenu de `git.properties` :

```yaml
management:
  info:
    git:
      mode: full
```

## Surcharge

Les valeurs du starter ont la priorité la plus basse : toute valeur posée par l'application (`application.yml`, variable d'environnement, argument) l'emporte.

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
server:
  shutdown: immediate
spring:
  threads:
    virtual:
      enabled: false
```

## Désactivation

```yaml
zbar:
  actuator:
    enabled: false
```

`enabled` vaut `true` par défaut. À `false`, le starter ne pose aucune propriété : les défauts de Spring Boot s'appliquent (seul `/actuator/health` est exposé, pas de tag `application`, pas de virtual threads).

## Limites

- Les endpoints exposés ne sont pas sécurisés par le starter. Protéger `/actuator/**` reste à la charge de l'application (Spring Security, port de management séparé, réseau).
- Virtual threads : depuis Java 24 (JEP 491), `synchronized` n'épingle plus le virtual thread à son thread porteur. Un appel natif (JNI, Foreign Function) l'épingle encore. Une bibliothèque qui fait des appels natifs bloquants peut donc saturer les threads porteurs sous charge.
- Le timeout de 30 s doit rester inférieur au délai d'arrêt de la plateforme. Sous Kubernetes, `terminationGracePeriodSeconds` vaut aussi 30 s par défaut : augmenter ce délai ou réduire le timeout.
