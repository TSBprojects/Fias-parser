package ru.neoflex.vak.fiasParser.dbfApi;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public class DbfDatabase {

    private final Logger log = LogManager.getLogger(DbfDatabase.class.getName());

    private ArrayList<DbfTable> tables;
    private Integer recordCount = 0;

    public Integer getRecordCount() {
        return recordCount;
    }

    public ArrayList<DbfTable> getTables() {
        return tables;
    }

    public DbfDatabase(Path fiasFilesFolder) throws IOException {
        if (!fiasFilesFolder.toFile().exists()) {
            log.error("The specified directory does not exist!");
            throw new IOException("Указанная директория не существует!");
        } else if (!isDbfFilesExist(fiasFilesFolder)) {
            log.error("There are no dbf files in the specified directory!");
            throw new IOException("В указанной директории отсутствуют dbf файлы!");
        }

        log.info("Define tables.");
        tables = new ArrayList<DbfTable>();
        try (Stream<Path> paths =
                     Files.find(fiasFilesFolder, 1,
                             (path, basicFileAttributes) ->
                                     !isTableAdded(Utils.getTableName(path.getFileName().toString())) &&
                                             path.getFileName().toString().toLowerCase().endsWith(".dbf"))) {
            paths.filter(Files::isRegularFile).forEach(path -> {
                tables.add(new DbfTable(path));
            });
            log.info("Initialize tables.");
            for (DbfTable table : tables) {
                table.initializeTable(fiasFilesFolder);
                recordCount += table.getRecordCount();
            }
        }
    }

    private boolean isDbfFilesExist(Path fiasFilesFolder) {
        File[] files = fiasFilesFolder.toFile().listFiles();
        for (File f : files) {
            if (f.getName().toLowerCase().endsWith(".dbf")) {
                return true;
            }
        }
        return false;
    }

    private boolean isTableAdded(String tableName) {
        for (DbfTable table : tables) {
            if (table.getTableName().equals(tableName)) {
                return true;
            }
        }
        return false;
    }
}
