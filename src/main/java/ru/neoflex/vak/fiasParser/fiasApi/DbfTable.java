package ru.neoflex.vak.fiasParser.fiasApi;

import com.linuxense.javadbf.DBFException;
import com.linuxense.javadbf.DBFField;
import com.linuxense.javadbf.DBFReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class DbfTable implements AutoCloseable {

    private Integer currentPart = 0;
    private DbfTablePart reader;

    private String dbtFilePath;
    private String firstPartName;
    private ArrayList<Header> headers;
    private ArrayList<DbfTablePart> parts;


    String getDbtFilePath() {
        return dbtFilePath;
    }

    public String getTableName() {
        return Utils.getTableName(firstPartName);
    }

    public ArrayList<Header> getHeaders() {
        return headers;
    }

    ArrayList<DbfTablePart> getParts() {
        return parts;
    }


    DbfTable(Path dbfPath) {
        firstPartName = dbfPath.getFileName().toString();
    }

    void initializeTable(Path rootPath) {
        headers = new ArrayList<Header>();
        parts = new ArrayList<DbfTablePart>();

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

        } catch (DBFException | IOException e) {
            e.printStackTrace();
        }

        try {
            findAllParts(rootPath);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private void findAllParts(Path rootPath) throws IOException {
//        try (Stream<Path> paths =
//                     Files.find(rootPath, 1,
//                             (path, basicFileAttributes) ->
//                                     isTablePart(this.getTableName(), path.getFileName().toString()))) {
//            paths.forEach(part -> parts.add(new DbfTablePart(part)));
//        }
        final File folder = new File(rootPath.toString());
        for (final File file : folder.listFiles()) {
            if (isTablePart(this.getTableName(), file.getName())) {
                if (file.getName().toLowerCase().endsWith(".dbt")) {
                    dbtFilePath = file.getPath();
                } else {
                    parts.add(new DbfTablePart(Paths.get(file.getPath()), this));
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
            throw new Exception("tableReader уже открыт!");
        }

        try {
            reader = parts.get(currentPart).startRead();
        } catch (DBFException e) {
            e.printStackTrace();
        }

        return this;
    }

    public ArrayList<String> nextRow() throws Exception {
        if (reader == null) {
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
