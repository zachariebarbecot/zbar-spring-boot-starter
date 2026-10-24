# zbar-transaction-spring-boot-starter

Starter Spring Boot pour gérer les transactions par programmation, sans `@Transactional`. Le bean `Transactions` expose trois opérations :

| Opération | Callback | Mode | Retour |
|---|---|---|---|
| `read(Supplier<T>)` | `Supplier<T>` | lecture seule | valeur du `Supplier` |
| `execute(Supplier<T>)` | `Supplier<T>` | écriture | valeur du `Supplier` |
| `run(Runnable)` | `Runnable` | écriture | aucun |

## Installation

La version du starter est gérée par `zbar-bom` (déjà importé par `zbar-parent`).

```xml
<dependency>
  <groupId>fr.zbar</groupId>
  <artifactId>zbar-transaction-spring-boot-starter</artifactId>
</dependency>
```

## Utilisation

```java
@Service
class OrderService {
  private final Transactions transactions;

  OrderService(Transactions transactions) {
    this.transactions = transactions;
  }

  Long place(Order order) {
    return transactions.execute(() -> repository.save(order).id());
  }

  Order find(Long id) {
    return transactions.read(() -> repository.findById(id).orElseThrow());
  }

  void purge() {
    transactions.run(repository::deleteAll);
  }
}
```

## Comportement

- Propagation `REQUIRED` fixe : un appel imbriqué rejoint la transaction en cours. Un `read` dans un `execute` ou un `run` rejoint donc la transaction en écriture.
- Rollback sur `RuntimeException` et `Error` uniquement ; l'exception est relancée telle quelle, sans enveloppe.
- Si un appel interne échoue et que l'appelant rattrape l'exception, l'appel externe lève une `UnexpectedRollbackException`.
- Pas d'exception checked, pas de timeout, pas de `REQUIRES_NEW` dans cette version.
- Le starter n'utilise aucune annotation `@Transactional`.

## Activation du bean

Le bean `Transactions` est créé si le contexte contient un seul `PlatformTransactionManager` (ou un seul marqué `@Primary`, qui est alors utilisé). Avec plusieurs managers sans `@Primary`, ou aucun manager, le contexte démarre sans bean `Transactions`.

## Propriétés

| Propriété | Défaut | Description |
|---|---|---|
| `zbar.transaction.enabled` | `true` | Active le starter. |

## Remplacer le bean

Un bean `Transactions` défini par l'application remplace celui du starter.

```java
@Bean
Transactions transactions(PlatformTransactionManager manager) {
  return new Transactions(manager);
}
```
