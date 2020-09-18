package com.ysbing.yrouter.core;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

/**
 * @author zhujiechen
 */
public class Configuration {
    private static final String TAG_ISSUE = "issue";
    private static final String ATTR_ID = "id";
    private static final String ATTR_VALUE = "value";
    private static final String TAG_BOOTCLASSPATH = "bootclasspath";

    public final List<String> bootClasspathList;
    private File xmlConfigFile;

    public Configuration(File xmlConfigFile) throws IOException, ParserConfigurationException, SAXException {
        this.bootClasspathList = new ArrayList<>();
        this.xmlConfigFile = xmlConfigFile;
        readXmlConfig(xmlConfigFile);
    }

    private void readXmlConfig(File xmlConfigFile) throws IOException, ParserConfigurationException, SAXException {
        if (!xmlConfigFile.exists()) {
            return;
        }

        System.out.printf("reading config file, %s\n", xmlConfigFile.getAbsolutePath());
        BufferedInputStream input = null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            input = new BufferedInputStream(new FileInputStream(xmlConfigFile));
            InputSource source = new InputSource(input);
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(source);
            NodeList issues = document.getElementsByTagName(TAG_ISSUE);
            for (int i = 0, count = issues.getLength(); i < count; i++) {
                Node node = issues.item(i);

                Element element = (Element) node;
                String id = element.getAttribute(ATTR_ID);
                if (id.length() == 0) {
                    System.err.println("Invalid config file: Missing required issue id attribute");
                    continue;
                }

                if (id.equals(TAG_BOOTCLASSPATH)) {
                    readBootClasspathFromXml(node);
                } else {
                    System.err.println("unknown issue " + id);
                }
            }
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    System.exit(-1);
                }
            }
        }
    }

    private void readBootClasspathFromXml(Node node) throws IOException {
        NodeList childNodes = node.getChildNodes();
        if (childNodes.getLength() > 0) {
            for (int j = 0, n = childNodes.getLength(); j < n; j++) {
                Node child = childNodes.item(j);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    Element check = (Element) child;
                    String value = check.getAttribute(ATTR_VALUE);
                    addBootList(value);
                }
            }
        }
    }

    private void addBootList(String item) throws IOException {
        if (item.length() == 0) {
            throw new IOException("Invalid config file: Missing required attribute " + ATTR_VALUE);
        }
        item = item.trim();
        File file = new File(item);
        if (!file.exists()) {
            file = new File(xmlConfigFile.getParentFile(), item);
        }
        if (file.exists()) {
            bootClasspathList.add(file.getAbsolutePath());
        }
    }

}

