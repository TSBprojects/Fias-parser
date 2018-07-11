package ru.neoflex.vak.fiasParser.fiasApi;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.stream.Stream;

public class FiasDatabase {

    //Logger log = LogManager.getLogger(FiasDatabase.class.getName());

    private ArrayList<DbfTable> tables;

    public ArrayList<DbfTable> getTables() {
        return tables;
    }

    public FiasDatabase(Path fiasFilesFolder) throws IOException {
        if (!fiasFilesFolder.toFile().exists()) {
            throw new IOException("Указанная директория не существует!");
        } else if (!isDbfFilesExist(fiasFilesFolder)) {
            throw new IOException("В указанной директории отсутствуют dbf файлы!");
        }

        tables = new ArrayList<DbfTable>();
        try (Stream<Path> paths =
                     Files.find(fiasFilesFolder, 1,
                             (path, basicFileAttributes) ->
                                     !isTableAdded(Utils.getTableName(path.getFileName().toString())) &&
                                             path.getFileName().toString().toLowerCase().endsWith(".dbf"))) {
            paths.filter(Files::isRegularFile).forEach(path -> tables.add(new DbfTable(path)));
        }
        tables.forEach(t -> t.initializeTable(fiasFilesFolder));
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
