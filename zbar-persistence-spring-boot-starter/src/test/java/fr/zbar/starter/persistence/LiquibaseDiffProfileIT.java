package fr.zbar.starter.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

class LiquibaseDiffProfileIT {

  private static final String MODULE = "zbar-persistence-spring-boot-starter";
  private static final String PLUGIN =
      "//project[artifactId='"
          + MODULE
          + "']/build/plugins/plugin[artifactId='liquibase-maven-plugin']";

  @Test
  void should_declare_plugin_with_hibernate_extension_and_yaml_output_when_profile_is_active()
      throws Exception {
    Document pom =
        effectivePom("with-profile", "-Pliquibase-diff", "-Dzbar.liquibase.entity-package=p");

    String pluginVersion = xpath(pom, PLUGIN + "/version");
    assertThat(pluginVersion).isNotBlank();
    assertThat(
            xpath(
                pom,
                PLUGIN + "/dependencies/dependency[artifactId='liquibase-hibernate7']/version"))
        .isEqualTo(pluginVersion);
    assertThat(xpath(pom, PLUGIN + "/configuration/diffChangeLogFile")).endsWith(".yaml");
  }

  @Test
  void should_bind_no_liquibase_execution_when_profile_is_inactive() throws Exception {
    Document pom = effectivePom("without-profile");

    assertThat(xpath(pom, "count(" + PLUGIN + "/executions/execution)")).isEqualTo("0");
    assertThat(xpath(pom, "count(" + PLUGIN + "/configuration)")).isEqualTo("0");
  }

  @Test
  void should_build_reference_url_from_entity_package_naming_strategies_and_default_dialect()
      throws Exception {
    Document pom =
        effectivePom("url", "-Pliquibase-diff", "-Dzbar.liquibase.entity-package=com.acme.entity");

    assertThat(xpath(pom, PLUGIN + "/configuration/referenceUrl"))
        .isEqualTo(
            "hibernate:spring:com.acme.entity?dialect=org.hibernate.dialect.PostgreSQLDialect"
                + "&hibernate.physical_naming_strategy="
                + "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy"
                + "&hibernate.implicit_naming_strategy="
                + "org.springframework.boot.hibernate.SpringImplicitNamingStrategy");
  }

  @Test
  void should_use_dialect_property_in_reference_url() throws Exception {
    Document pom =
        effectivePom(
            "dialect",
            "-Pliquibase-diff",
            "-Dzbar.liquibase.entity-package=com.acme.entity",
            "-Dzbar.liquibase.dialect=org.hibernate.dialect.H2Dialect");

    assertThat(xpath(pom, PLUGIN + "/configuration/referenceUrl"))
        .contains("?dialect=org.hibernate.dialect.H2Dialect&");
  }

  @Test
  void should_fail_explicitly_when_entity_package_is_missing() throws Exception {
    Result result = run(mvn("-Pliquibase-diff", "initialize"));

    assertThat(result.exit()).isNotZero();
    assertThat(result.output()).contains("zbar.liquibase.entity-package");
  }

  private static String xpath(Document pom, String expression) throws Exception {
    return XPathFactory.newInstance().newXPath().evaluate(expression, pom).strip();
  }

  private static Document effectivePom(String name, String... args) throws Exception {
    Path output = Path.of("target", "effective-pom-" + name + ".xml").toAbsolutePath();
    List<String> command = mvn("help:effective-pom", "-Doutput=" + output);
    command.addAll(List.of(args));
    Result result = run(command);
    assertThat(result.exit()).as(result.output()).isZero();
    return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(output.toFile());
  }

  private static List<String> mvn(String... args) {
    Path root = Path.of("..").toAbsolutePath().normalize();
    List<String> command =
        new ArrayList<>(
            List.of(
                root.resolve("mvnw").toString(),
                "-q",
                "-B",
                "-ntp",
                "-f",
                root.resolve("pom.xml").toString(),
                "-pl",
                MODULE,
                "-am"));
    command.addAll(List.of(args));
    return command;
  }

  private static Result run(List<String> command) throws IOException, InterruptedException {
    Process process = new ProcessBuilder(command).redirectErrorStream(true).start();
    String output = new String(process.getInputStream().readAllBytes());
    return new Result(process.waitFor(), output);
  }

  private record Result(int exit, String output) {}
}
