# zbar-logs-spring-boot-starter

Starter Spring Boot pour des logs homogènes, sans configuration.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-logs-spring-boot-starter</artifactId>
</dependency>
```

## Désactivation

```yaml
zbar:
  logs:
    enabled: false
```

`enabled` vaut `true` par défaut. À `false`, le starter ne crée aucun bean.

## Propriétés

| Propriété | Défaut | Description |
|---|---|---|
| `zbar.logs.enabled` | `true` | Active le starter. |
| `zbar.logs.exclude-paths` | `/actuator/**`, `/v3/api-docs` | Chemins sans logs début/fin ; la liste remplace le défaut. |
