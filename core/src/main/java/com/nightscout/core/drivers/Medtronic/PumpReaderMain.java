package com.nightscout.core.drivers.Medtronic;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.RandomAccessFile;

public class PumpReaderMain {

  private final static Logger log = LoggerFactory.getLogger(PumpReaderMain.class);

  public static void main(String[] args) {
    if (args.length < 2) {
      log.error("Not enough arguments. Expected <path> <model>");
      return;
    }
    log.info("Got file path: {}", args[0]);
    log.info("Got pump model: {}", args[1]);

    try {
      new Page(readDataFile(args[0]), parsePumpModel(args[1]));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static byte[] readDataFile(String filename) throws IOException {
    RandomAccessFile f = new RandomAccessFile(filename, "r");
    byte[] data;

    try {
      long longlength = f.length();
      int length = (int) longlength;
      if (length != longlength) {
        throw new IOException("File size >= 2GB");
      }

      data = new byte[length];
      f.readFully(data);
    } finally {
      f.close();
    }
    return data;

  }
  private static PumpModel parsePumpModel(String pumpModel) {
    try {
      return PumpModel.valueOf(pumpModel);
    } catch (Exception e) {
      log.warn("Couldn't parse pump model {}, using MM523 instead.", pumpModel);
      return PumpModel.MM523;
    }
  }
}
