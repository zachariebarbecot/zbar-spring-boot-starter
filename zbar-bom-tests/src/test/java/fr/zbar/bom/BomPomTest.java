package fr.zbar.bom;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class BomPomTest {

  private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

  @Test
  void rootIsPomAggregatorDeclaringZbarBom() throws Exception {
    Element project = read(ROOT.resolve("pom.xml"));

    assertThat(childText(project, "packaging")).isEqualTo("pom");
    assertThat(texts(child(project, "modules"), "module")).contains("zbar-bom");
  }

  @Test
  void bomHasNoParentIsPomAndImportsThreeBoms() throws Exception {
    Element project = read(ROOT.resolve("zbar-bom/pom.xml"));

    assertThat(child(project, "parent")).isNull();
    assertThat(childText(project, "packaging")).isEqualTo("pom");
    assertThat(importedBoms(project))
        .containsExactlyInAnyOrder(
            "org.springframework.boot:spring-boot-dependencies",
            "org.testcontainers:testcontainers-bom",
            "io.cucumber:cucumber-bom");
  }

  @Test
  void effectivePomManagesVersionOfCoreLibraries() throws Exception {
    Path output = Files.createTempFile("zbar-bom-effective", ".xml");
    Process process =
        new ProcessBuilder(
                ROOT.resolve("mvnw").toString(),
                "-q",
                "-B",
                "-ntp",
                "-f",
                ROOT.resolve("zbar-bom/pom.xml").toString(),
                "help:effective-pom",
                "-Doutput=" + output)
            .redirectErrorStream(true)
            .start();
    String log = new String(process.getInputStream().readAllBytes());
    assertThat(process.waitFor()).as(log).isZero();

    Element managed = child(child(read(output), "dependencyManagement"), "dependencies");
    for (String artifactId :
        List.of(
            "commons-lang3",
            "commons-collections4",
            "testcontainers",
            "cucumber-java",
            "nimbus-jose-jwt")) {
      assertThat(managedVersion(managed, artifactId)).as(artifactId).isNotBlank();
    }
  }

  @Test
  void bomDeclaresNoBouncyCastleEntry() throws Exception {
    assertThat(Files.readString(ROOT.resolve("zbar-bom/pom.xml"))).doesNotContain("org.bouncycastle");
  }

  private static String managedVersion(Element managed, String artifactId) {
    NodeList nodes = managed.getElementsByTagName("dependency");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element dependency = (Element) nodes.item(i);
      if (artifactId.equals(childText(dependency, "artifactId"))) {
        return childText(dependency, "version");
      }
    }
    return null;
  }

  private static List<String> importedBoms(Element project) {
    List<String> result = new ArrayList<>();
    Element managed = child(child(project, "dependencyManagement"), "dependencies");
    if (managed == null) {
      return result;
    }
    NodeList nodes = managed.getElementsByTagName("dependency");
    for (int i = 0; i < nodes.getLength(); i++) {
      Element dependency = (Element) nodes.item(i);
      if ("import".equals(childText(dependency, "scope"))
          && "pom".equals(childText(dependency, "type"))) {
        result.add(childText(dependency, "groupId") + ":" + childText(dependency, "artifactId"));
      }
    }
    return result;
  }

  private static Element read(Path pom) throws Exception {
    Document document =
        DocumentBuilderFactory.newDefaultInstance().newDocumentBuilder().parse(pom.toFile());
    return document.getDocumentElement();
  }

  private static Element child(Element parent, String name) {
    if (parent == null) {
      return null;
    }
    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element element && element.getTagName().equals(name)) {
        return element;
      }
    }
    return null;
  }

  private static String childText(Element parent, String name) {
    Element element = child(parent, name);
    return element == null ? null : element.getTextContent().trim();
  }

  private static List<String> texts(Element parent, String name) {
    List<String> result = new ArrayList<>();
    if (parent == null) {
      return result;
    }
    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element element && element.getTagName().equals(name)) {
        result.add(element.getTextContent().trim());
      }
    }
    return result;
  }
}
