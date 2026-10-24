package fr.zbar.starter.test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import fr.zbar.starter.test.fixtures.custom.core.CoreService;
import fr.zbar.starter.test.fixtures.custom.core.SpringCoreService;
import org.junit.jupiter.api.Test;

class ZbarArchitectureRulesTest {

  private static final String FIXTURES = "fr.zbar.starter.test.fixtures.";

  private final ZbarArchitectureRules rules = new ZbarArchitectureRules();

  @Test
  void should_pass_when_domain_depends_on_neither_spring_nor_adapter() {
    JavaClasses classes = new ClassFileImporter().importPackages(FIXTURES + "clean");

    assertThatCode(() -> rules.check(classes)).doesNotThrowAnyException();
  }

  @Test
  void should_fail_naming_class_and_spring_dependency_when_domain_depends_on_spring() {
    JavaClasses classes = new ClassFileImporter().importPackages(FIXTURES + "springfault");

    assertThatThrownBy(() -> rules.check(classes))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SpringAwareService")
        .hasMessageContaining("org.springframework.context.ApplicationContext");
  }

  @Test
  void should_fail_naming_class_and_adapter_when_domain_depends_on_adapter() {
    JavaClasses classes = new ClassFileImporter().importPackages(FIXTURES + "adapterfault");

    assertThatThrownBy(() -> rules.check(classes))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("GatewayUser")
        .hasMessageContaining("adapterfault.adapter.Gateway");
  }

  @Test
  void should_apply_custom_packages_when_configured() {
    JavaClasses classes = new ClassFileImporter().importClasses(CoreService.class);
    ZbarArchitectureRules custom = new ZbarArchitectureRules("..core..", "..infra..");

    assertThatThrownBy(() -> custom.check(classes))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("CoreService")
        .hasMessageContaining("custom.infra.Database");
  }

  @Test
  void should_apply_custom_domain_package_to_spring_rule_when_configured() {
    JavaClasses classes = new ClassFileImporter().importClasses(SpringCoreService.class);
    ZbarArchitectureRules custom = new ZbarArchitectureRules("..core..", "..infra..");

    assertThatThrownBy(() -> custom.check(classes))
        .isInstanceOf(AssertionError.class)
        .hasMessageContaining("SpringCoreService")
        .hasMessageContaining("org.springframework.context.ApplicationContext");
  }

  @Test
  void should_use_default_packages_when_none_configured() {
    JavaClasses classes = new ClassFileImporter().importClasses(CoreService.class);

    assertThatCode(() -> rules.check(classes)).doesNotThrowAnyException();
  }
}
