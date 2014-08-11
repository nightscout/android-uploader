package com.nightscout.android.dexcom.records;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;

public class GenericXMLRecord implements Serializable {
    int XML_START = 36;
    int XML_END = 241;

    private Element xmlElement;

    public GenericXMLRecord(byte[] packet) {
        Document document;
        String xml = new String(Arrays.copyOfRange(packet, XML_START, XML_END));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder;
        try
        {
            builder = factory.newDocumentBuilder();
            document = builder.parse(new InputSource(new StringReader(xml)));
            xmlElement = document.getDocumentElement();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // example: String sn = getXmlElement().getAttribute("SerialNumber");
    public Element getXmlElement() {
        return xmlElement;
    }
}
