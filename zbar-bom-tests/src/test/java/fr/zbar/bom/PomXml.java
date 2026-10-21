package fr.zbar.bom;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class PomXml {

  static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

  private PomXml() {}

  static Element read(Path pom) throws Exception {
    return DocumentBuilderFactory.newDefaultInstance()
        .newDocumentBuilder()
        .parse(pom.toFile())
        .getDocumentElement();
  }

  static List<Element> children(Element parent, String name) {
    List<Element> result = new ArrayList<>();
    if (parent == null) {
      return result;
    }
    NodeList nodes = parent.getChildNodes();
    for (int i = 0; i < nodes.getLength(); i++) {
      if (nodes.item(i) instanceof Element element && element.getTagName().equals(name)) {
        result.add(element);
      }
    }
    return result;
  }

  static Element child(Element parent, String name) {
    return children(parent, name).stream().findFirst().orElse(null);
  }

  static String childText(Element parent, String name) {
    Element element = child(parent, name);
    return element == null ? null : element.getTextContent().trim();
  }

  static List<String> texts(Element parent, String name) {
    return children(parent, name).stream().map(e -> e.getTextContent().trim()).toList();
  }

  static List<Element> managedDependencies(Element project) {
    return children(child(child(project, "dependencyManagement"), "dependencies"), "dependency");
  }

  static List<String> importedBoms(Element project) {
    return managedDependencies(project).stream()
        .filter(
            dependency ->
                "import".equals(childText(dependency, "scope"))
                    && "pom".equals(childText(dependency, "type")))
        .map(
            dependency ->
                childText(dependency, "groupId")
                    + ":"
                    + childText(dependency, "artifactId")
                    + ":"
                    + childText(dependency, "version"))
        .toList();
  }

  /** Effective POM of a reactor module, resolved with its upstream modules. */
  static Element effectivePom(String module) throws Exception {
    Path output = Files.createTempFile("zbar-effective", ".xml");
    Process process =
        new ProcessBuilder(
                ROOT.resolve("mvnw").toString(),
                "-q",
                "-B",
                "-ntp",
                "-f",
                ROOT.resolve("pom.xml").toString(),
                "-pl",
                module,
                "-am",
                "help:effective-pom",
                "-Doutput=" + output)
            .redirectErrorStream(true)
            .start();
    String log = new String(process.getInputStream().readAllBytes());
    if (process.waitFor() != 0) {
      throw new IllegalStateException(log);
    }
    Element document = read(output);
    if (document.getTagName().equals("project")) {
      return document;
    }
    List<Element> projects = children(document, "project");
    return projects.get(projects.size() - 1);
  }
}
