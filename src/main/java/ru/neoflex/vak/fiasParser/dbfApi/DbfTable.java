package ru.neoflex.vak.fiasParser.dbfApi;

import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DbfTable implements AutoCloseable {

    private final Logger log = LogManager.getLogger(DbfTable.class.getName());

    private Integer currentPart = 0;
    private DbfTablePart reader;

    private String tableName;
    private String dbtFilePath;
    private String firstPartName;
    private ArrayList<Header> headers;
    private ArrayList<DbfTablePart> parts;
    private Integer recordCount = 0;


    String getDbtFilePath() {
        return dbtFilePath;
    }

    public Integer getRecordCount() {
        return recordCount;
    }

    public String getTableName() {
        return tableName;
    }

    public ArrayList<Header> getHeaders() {
        return headers;
    }

    ArrayList<DbfTablePart> getParts() {
        return parts;
    }


    DbfTable(Path dbfPath) {
        firstPartName = dbfPath.getFileName().toString();
        tableName = Utils.getTableName(firstPartName);
    }

    void initializeTable(Path rootPath) throws IOException {
        headers = new ArrayList<>();
        parts = new ArrayList<>();

        String tablePart = rootPath.toString().toLowerCase() + "\\" + firstPartName.toLowerCase();
        try (DBFReader reader = new DBFReader(new FileInputStream(tablePart))) {
            for (int i = 0; i < reader.getFieldCount(); i++) {

                DBFField column = reader.getField(i);
                String dataType = column.getType().toString();
                String dataTypeSize = Integer.toString(column.getLength());

                switch (dataType) {
                    case "MEMO":
                        dataType = "TEXT";
                        dataTypeSize = "";
                        break;
                    case "DATE":
                        dataTypeSize = "";
                        break;
                    case "LONG":
                        dataType = "INTEGER";
                        dataTypeSize = "";
                        break;
                    case "CHARACTER":
                        dataType = "NVARCHAR";
                        break;
                }

                Header header = new Header(column.getName(), dataType, dataTypeSize);
                headers.add(header);
            }
        }
        findAllParts(rootPath);
    }

    private void findAllParts(Path rootPath) throws IOException {
//        try (Stream<Path> paths =
//                     Files.find(rootPath, 1,
//                             (path, basicFileAttributes) ->
//                                     isTablePart(this.getTableName(), path.getFileName().toString()))) {
//            paths.forEach(part -> parts.add(new DbfTablePart(part)));
//        }
        log.info("Search all parts of the table '" + tableName + "'");
        final File folder = new File(rootPath.toString());
        for (final File file : folder.listFiles()) {
            if (isTablePart(this.getTableName(), file.getName())) {
                if (file.getName().toLowerCase().endsWith(".dbt")) {
                    dbtFilePath = file.getPath();
                } else {
                    DbfTablePart tablePart = new DbfTablePart(Paths.get(file.getPath()), this);
                    recordCount += tablePart.getRecordCount();
                    parts.add(tablePart);
                }
            }
        }
    }

    private boolean isTablePart(String tableName, String target) {
        return tableName.toLowerCase().equals(Utils.getTableName(target.toLowerCase()));
    }

    public class Header {
        private Header(String name, String dataType, String dataTypeSize) {
            this.name = name;
            this.dataType = dataType;
            this.dataTypeSize = dataTypeSize;
        }

        public String name;
        public String dataType;
        public String dataTypeSize;
    }


    public DbfTable startRead() throws Exception {
        if (reader != null && currentPart == 0) {
            log.error("tableReader is already open!");
            throw new Exception("tableReader уже открыт!");
        }
        log.info("Start to read records! (table part num" + currentPart + ")");
        reader = parts.get(currentPart).startRead();
        return this;
    }

    public ArrayList<String> nextRow() throws Exception {
        if (reader == null) {
            log.error("This tableReader is closed!");
            throw new Exception("Этот tableReader закрыт!");
        }

        ArrayList<String> rowObjects;
        rowObjects = reader.nextRow();
        if (rowObjects == null) {
            currentPart++;
            if (currentPart < parts.size()) {
                startRead();
                rowObjects = reader.nextRow();
            } else {
                return null;
            }
        }
        return rowObjects;
    }

    @Override
    public void close() throws Exception {
        if (reader == null) {
            throw new Exception("tableReader уже закрыт!");
        }

        currentPart = 0;
        reader.close();
        reader = null;
    }
}
