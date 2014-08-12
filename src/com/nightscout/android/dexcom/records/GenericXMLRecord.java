package com.nightscout.android.dexcom.records;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.Serializable;
import java.io.StringReader;
import java.util.Arrays;

public class GenericXMLRecord extends GenericTimestampRecord implements Serializable {
    int XML_START = 8;
    int XML_END = 241;

    private Element xmlElement;

    public GenericXMLRecord(byte[] packet) {
        super(Arrays.copyOfRange(packet, 0, 7));
        Document document;
        // TODO: it would be best if we could just remove /x00 character and read till end
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
