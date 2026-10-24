package fr.zbar.starter.test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;

public final class ZbarArchitectureRules {

  private final String domainPackage;
  private final String adapterPackage;

  public ZbarArchitectureRules() {
    this("..domain..", "..adapter..");
  }

  public ZbarArchitectureRules(String domainPackage, String adapterPackage) {
    this.domainPackage = domainPackage;
    this.adapterPackage = adapterPackage;
  }

  public void check(JavaClasses classes) {
    checkDomainAvoids("org.springframework..", classes);
    checkDomainAvoids(adapterPackage, classes);
  }

  private void checkDomainAvoids(String forbiddenPackage, JavaClasses classes) {
    noClasses()
        .that()
        .resideInAPackage(domainPackage)
        .should()
        .dependOnClassesThat()
        .resideInAPackage(forbiddenPackage)
        .allowEmptyShould(true)
        .check(classes);
  }
}
