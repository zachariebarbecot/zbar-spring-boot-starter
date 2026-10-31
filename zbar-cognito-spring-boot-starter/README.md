# zbar-cognito-spring-boot-starter

Starter Spring Boot qui protège toutes les routes de l'application par un jeton d'accès AWS Cognito, sans écrire de configuration de sécurité.

- Resource server JWT stateless : pas de session, pas de CSRF.
- Seul l'access token est accepté : signature (JWKS de l'issuer), issuer, expiration, `token_use=access` et `client_id` dans la liste autorisée sont contrôlés. Un access token Cognito n'a pas de claim `aud`.
- Toute route exige un jeton, sauf `<base-path actuator>/health/**`, la documentation OpenAPI et Swagger UI.
- Les groupes Cognito deviennent des rôles `ROLE_<groupe>`.
- Hors périmètre : login, session, refresh, CORS, ID tokens.

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-cognito-spring-boot-starter</artifactId>
</dependency>
```

Le starter apporte `spring-boot-starter-security-oauth2-resource-server` en transitif. Il vise une application Spring MVC (servlet).

## Configuration

```yaml
zbar:
  cognito:
    region: eu-west-1
    user-pool-id: eu-west-1_AbCdEf
    allowed-client-ids:
      - 1h57kf5cpq17m0eml12EXAMPLE
```

| Propriété | Défaut | Rôle |
|---|---|---|
| `zbar.cognito.enabled` | `true` | Active ou coupe le starter. |
| `zbar.cognito.issuer-uri` | issuer dérivé | Issuer des jetons. Prioritaire sur `region` + `user-pool-id`. |
| `zbar.cognito.region` | aucun | Région AWS du user pool. |
| `zbar.cognito.user-pool-id` | aucun | Identifiant du user pool. |
| `zbar.cognito.allowed-client-ids` | aucun (obligatoire) | `client_id` acceptés dans le jeton. |
| `zbar.cognito.groups-claim` | `cognito:groups` | Claim qui porte les groupes de l'utilisateur. |

Sans `issuer-uri`, l'issuer vaut `https://cognito-idp.<region>.amazonaws.com/<user-pool-id>`. Les clés de signature sont lues à `<issuer>/.well-known/jwks.json`.

Au démarrage, l'application échoue avec un message qui nomme la propriété manquante si ni `issuer-uri` ni `region` + `user-pool-id` ne sont posés, ou si `allowed-client-ids` est vide.

## Routes publiques

| Route | Chemin |
|---|---|
| Santé | `${management.endpoints.web.base-path}/health/**` (défaut `/actuator/health/**`) |
| Documentation OpenAPI | `${springdoc.api-docs.path}/**` (défaut `/v3/api-docs/**`) |
| Swagger UI | `${springdoc.swagger-ui.path}/**` (défaut `/swagger-ui.html`) et `/swagger-ui/**` |

## Remplacement et extension

- Une application qui déclare sa propre `SecurityFilterChain` remplace la chaîne du starter (bean `cognitoSecurityFilterChain`).
- Un bean `JwtDecoder` ou `JwtAuthenticationConverter` défini par l'application est utilisé à la place de celui du starter.

## Groupes et rôles

Chaque groupe du claim `cognito:groups` devient une autorité `ROLE_<groupe>`, casse conservée : `["Admin", "ops"]` donne `ROLE_Admin` et `ROLE_ops`. Sans ce claim, l'utilisateur est authentifié sans aucun rôle. Le starter n'impose aucune règle d'accès : à l'application de les poser.

```java
@GetMapping("/admin")
@PreAuthorize("hasRole('Admin')")
String admin() { ... }
```

## Désactivation

```yaml
zbar:
  cognito:
    enabled: false
```

À `false`, le starter ne crée aucun bean.
