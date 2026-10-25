# zbar-spring-boot-starter

Bibliothèque de starters Spring Boot (`fr.zbar`) appliquant des défauts de production sans configuration.

## Consommer les starters via GitHub Packages

Les versions sont publiées dans GitHub Packages à chaque tag `vX.Y.Z` (version Maven `X.Y.Z`).

1. Créer un Personal Access Token (classic) avec le seul scope `read:packages`.
2. Déclarer le dépôt et les identifiants dans `~/.m2/settings.xml` :

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>VOTRE_UTILISATEUR_GITHUB</username>
      <password>VOTRE_PAT</password>
    </server>
  </servers>
  <profiles>
    <profile>
      <id>zbar</id>
      <repositories>
        <repository>
          <id>github</id>
          <url>https://maven.pkg.github.com/zachariebarbecot/zbar-spring-boot-starter</url>
        </repository>
      </repositories>
    </profile>
  </profiles>
  <activeProfiles>
    <activeProfile>zbar</activeProfile>
  </activeProfiles>
</settings>
```

3. Importer le BOM puis ajouter les starters voulus :

```xml
<dependencyManagement>
  <dependencies>
    <dependency>
      <groupId>fr.zbar</groupId>
      <artifactId>zbar-bom</artifactId>
      <version>X.Y.Z</version>
      <type>pom</type>
      <scope>import</scope>
    </dependency>
  </dependencies>
</dependencyManagement>
```

Chaque starter a son propre `README.md` (installation, comportement par défaut, propriétés, désactivation).
