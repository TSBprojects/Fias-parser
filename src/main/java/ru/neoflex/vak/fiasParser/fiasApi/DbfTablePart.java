package ru.neoflex.vak.fiasParser.fiasApi;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;

public class DbfTablePart implements AutoCloseable {

    private DBFReader reader;

    private DbfTable table;
    private Path partPath;

    DbfTablePart(Path tablePartPath, DbfTable table) {
        this.table = table;
        partPath = tablePartPath;
    }


    DbfTablePart startRead() throws Exception {
        if (reader != null) {
            throw new Exception("tablePartReader уже открыт!");
        }

        try {
            reader = new DBFReader(new FileInputStream(partPath.toString()));

            String dbtPath = table.getDbtFilePath();
            if (dbtPath != null) {
                reader.setMemoFile(new File(dbtPath));
            }

        } catch (DBFException | IOException e) {
            e.printStackTrace();
        }
        return this;
    }

    ArrayList<String> nextRow() throws Exception {
        if (reader == null) {
            throw new Exception("Этот tablePartReader закрыт!");
        }

        Object[] rowObjects;
        ArrayList<String> rObjects = new ArrayList<>();

        rowObjects = reader.nextRecord();
        if (rowObjects == null) {
            return null;
        }

        ArrayList<DbfTable.Header> headers = table.getHeaders();
        for (int i = 0; i < rowObjects.length; i++) {
            if (rowObjects[i] != null) {
                if (headers.get(i).dataType.equals("DATE")) {
                    rObjects.add(Utils.getTimeFromString(rowObjects[i].toString()));
                } else {
                    String f = changeEncoding(rowObjects[i].toString());
                    rObjects.add(f);
                }
            } else {
                rObjects.add(null);
            }
        }
        return rObjects;
    }

    private String changeEncoding(String str) throws UnsupportedEncodingException {
        return new String(str.getBytes("ISO-8859-1"), "Cp866");
    }

    @Override
    public void close() throws Exception {
        if (reader == null) {
            throw new Exception("tablePartReader уже закрыт!");
        }

        DBFUtils.close(reader);
        reader = null;
    }
}
