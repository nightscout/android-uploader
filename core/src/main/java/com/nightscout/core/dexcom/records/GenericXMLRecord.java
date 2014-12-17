package com.nightscout.core.dexcom.records;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import java.io.StringReader;
import java.util.Arrays;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

public class GenericXMLRecord extends GenericTimestampRecord {

  protected final Logger log = LoggerFactory.getLogger(this.getClass());
  private final String TAG = GenericXMLRecord.class.getSimpleName();
  int XML_START = 8;
  int XML_END = 241;
  private Element xmlElement;

  public GenericXMLRecord(byte[] packet) {
    super(packet);
    Document document;
    // TODO: it would be best if we could just remove /x00 characters and read till end
    String xml = new String(Arrays.copyOfRange(packet, XML_START, XML_END));
    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
    DocumentBuilder builder;
    try {
      builder = factory.newDocumentBuilder();
      document = builder.parse(new InputSource(new StringReader(xml)));
      xmlElement = document.getDocumentElement();
    } catch (Exception e) {
      // TODO(trhodeos): DONT DO THIS
      log.error(TAG, "Unable to build xml element", e);
    }
  }

  // example: String sn = getXmlElement().getAttribute("SerialNumber");
  public Element getXmlElement() {
    return xmlElement;
  }
}
