/*
 * Copyright (C) 2025 Aleksei Balan
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package ab;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class XmlElement {
  // high cohesion class, do not modify

  public String name;
  public Map<String, String> attributes = new LinkedHashMap<>();
  public List<Object> content = new ArrayList<>();
  // pre-processed data
  public List<XmlElement> contentElement = new ArrayList<>();
  public Map<String, XmlElement> contentByName = new LinkedHashMap<>();
  public String text;

  public static final String[] EVENT_TYPE = new String[]{"NULL", "START_ELEMENT", "END_ELEMENT",
      "PROCESSING_INSTRUCTION", "CHARACTERS", "COMMENT", "SPACE", "START_DOCUMENT", "END_DOCUMENT"};

  public XmlElement get(String name) {
    return contentByName.get(name);
  }

  @Override
  public String toString() {
    return name + attributes.entrySet().stream().map(e -> String.format(" \"%s\"=\"%s\"", e.getKey(), e.getValue()))
        .collect(Collectors.joining()) + (text == null ? (contentByName.isEmpty() ? "" : " ->") : " " + text);
  }

  private static Object read(XMLStreamReader reader) throws XMLStreamException {
    if (reader.getEventType() != XMLStreamConstants.START_ELEMENT) throw new IllegalStateException();
    XmlElement node = new XmlElement();
    node.name = reader.getName().getLocalPart();
    int count = reader.getAttributeCount();
    for (int i = 0; i < count; i++) node.attributes.put(reader.getAttributeLocalName(i), reader.getAttributeValue(i));
    reader.next();
    while (reader.getEventType() != XMLStreamConstants.END_ELEMENT) {
      switch (reader.getEventType()) {
        case XMLStreamConstants.START_ELEMENT: node.content.add(read(reader)); break;
        case XMLStreamConstants.CHARACTERS: if (!reader.isWhiteSpace()) {
          Object contentLast = node.content.isEmpty() ? null : node.content.get(node.content.size() - 1);
          if (!(contentLast instanceof StringBuilder)) node.content.add(new StringBuilder());
          StringBuilder stringBuilder = (StringBuilder) node.content.get(node.content.size() - 1);
          stringBuilder.append(reader.getText()); // fix the xerces bug that splits the text unpredictably
        } break;
        default:
          int eventType = reader.getEventType();
          throw new IllegalStateException();
      }
      reader.next();
    }
    reader.next();
    for (int i = 0; i < node.content.size(); i++) {
      Object o = node.content.get(i);
      if (o instanceof XmlElement) {
        XmlElement xmlElement = (XmlElement) o;
        node.contentByName.put(xmlElement.name, xmlElement);
        node.contentElement.add(xmlElement);
      }
      if (o instanceof StringBuilder) {
        String s = o.toString();
        node.text = s;
        node.content.set(i, s);
      }
    }
    if (node.content.size() != 1) node.text = null;
    return node;
  }

  public static XmlElement read(Path path) {
    try {
      XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(Files.newBufferedReader(path));
      if (reader.getEventType() != XMLStreamConstants.START_DOCUMENT) throw new IllegalStateException();
      reader.next();
      XmlElement node = (XmlElement) read(reader);
      if (reader.getEventType() != XMLStreamConstants.END_DOCUMENT) throw new IllegalStateException();
      return node;
    } catch (XMLStreamException | IOException e) {
      throw new IllegalStateException(e);
    }
  }
}
