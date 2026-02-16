package ru.uni.lab.service;

import ru.uni.lab.model.ParameterInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.TreeMap;
import java.util.Map;

public class XmlParser {

    public Map<Integer, ParameterInfo> parse(File file) throws Exception {
        TreeMap<Integer, ParameterInfo> paramMap = new TreeMap<>();
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(file);
        
        Element root = doc.getDocumentElement();
        parseParamElementsRecursive(root.getChildNodes(), paramMap);
        
        return paramMap;
    }

    private void parseParamElementsRecursive(NodeList nodes, Map<Integer, ParameterInfo> paramMap) {
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node instanceof Element) {
                Element element = (Element) node;
                if ("Param".equals(element.getTagName())) {
                    String name = element.getAttribute("name");
                    String numberStr = element.getAttribute("number");
                    ParameterInfo info = null;
                    
                    if (numberStr != null && !numberStr.isEmpty()) {
                        try {
                            int number = Integer.parseInt(numberStr);
                            if (name != null && !name.isEmpty()) {
                                info = new ParameterInfo(number, name);
                                paramMap.put(number, info);
                            }
                        } catch (NumberFormatException e) {
                            // Ignore invalid numbers
                        }
                    }
                    
                    // Process direct children for metadata (Description, Textes)
                    // We iterate again or do it inside the loop?
                    // Since we are iterating `nodes` (siblings), we need to get children of THIS element.
                    NodeList children = element.getChildNodes();
                    
                    if (info != null) {
                        for (int j = 0; j < children.getLength(); j++) {
                            Node child = children.item(j);
                            if (child instanceof Element) {
                                Element childEl = (Element) child;
                                if ("Description".equals(childEl.getTagName())) {
                                    info.setDescription(childEl.getTextContent());
                                } else if ("Textes".equals(childEl.getTagName())) {
                                    NodeList textNodes = childEl.getElementsByTagName("Text");
                                    for (int k = 0; k < textNodes.getLength(); k++) {
                                        Node textNode = textNodes.item(k);
                                        if (textNode instanceof Element) {
                                            Element textEl = (Element) textNode;
                                            String numStr = textEl.getAttribute("number");
                                            String textVal = textEl.getTextContent();
                                            try {
                                                int num = Integer.parseInt(numStr);
                                                info.addValue(num, textVal);
                                            } catch (Exception e) {
                                                // Ignore
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Recursively process children to find nested Param elements
                    if (element.hasChildNodes()) {
                        parseParamElementsRecursive(children, paramMap);
                    }
                } else {
                    // If it's not a Param, recurse into children
                    if (element.hasChildNodes()) {
                        parseParamElementsRecursive(element.getChildNodes(), paramMap);
                    }
                }
            }
        }
    }
}
