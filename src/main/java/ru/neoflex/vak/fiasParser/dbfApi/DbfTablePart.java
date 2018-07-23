package ru.neoflex.vak.fiasParser.dbfApi;

import com.linuxense.javadbf.DBFReader;
import com.linuxense.javadbf.DBFUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DbfTablePart implements AutoCloseable {

    private final Logger log = LogManager.getLogger(DbfTablePart.class.getName());

    private DBFReader reader;

    private DbfTable table;
    private Path partPath;
    private Integer recordCount;

    public Integer getRecordCount() {
        return recordCount;
    }

    DbfTablePart(Path tablePartPath, DbfTable table) throws FileNotFoundException {
        this.table = table;
        partPath = tablePartPath;
        recordCount = new DBFReader(new FileInputStream(partPath.toString())).getRecordCount();
    }


    DbfTablePart startRead() throws Exception {
        if (reader != null) {
            log.error("tablePartReader is already open!");
            throw new Exception("tablePartReader уже открыт!");
        }

        reader = new DBFReader(new FileInputStream(partPath.toString()));

        String dbtPath = table.getDbtFilePath();
        if (dbtPath != null) {
            reader.setMemoFile(new File(dbtPath));
        }

        return this;
    }

    ArrayList<String> nextRow() throws Exception {
        if (reader == null) {
            log.error("This tablePartReader is closed!");
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
                    rObjects.add(changeEncoding(rowObjects[i].toString()));
                }
            } else {
                rObjects.add(null);
            }
        }
        return rObjects;
    }

    private String changeEncoding(String str) throws UnsupportedEncodingException {
        String res;
        Pattern p = Pattern.compile("^[а-яА-ЯёЁ\\d\\s\\p{Punct}]*$");
        Matcher m = p.matcher(str);
        if (m.matches()) {
            res = str;
        } else {
            res = new String(str.getBytes("ISO-8859-1"), "Cp866");
        }
        return res.replace("'", "\"");
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
