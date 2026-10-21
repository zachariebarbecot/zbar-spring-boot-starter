package fr.zbar.bom;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

final class PomXml {

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
}
