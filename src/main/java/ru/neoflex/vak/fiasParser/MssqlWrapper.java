package ru.neoflex.vak.fiasParser;

import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.fiasApi.DbfTable;

import java.io.IOException;
import java.sql.Statement;
import java.util.ArrayList;

public class MssqlWrapper extends DbWrapper implements AutoCloseable {

    MssqlWrapper(MssqlProperties config) {
        super(config);
    }

    @Override
    void fillDbfTable(DbfTable table) throws Exception {
        Integer recordsCount = 0;
        Integer rowsInChunk = 0;
        String pattern = getInsertPattern(table);
        StringBuilder execSql = new StringBuilder("");
        ArrayList<DbfTable.Header> headers = table.getHeaders();

        Statement st = createStatement();

        System.out.print("Формируем chunk... ");
        ArrayList<String> rowObjects;
        try (DbfTable reader = table.startRead()) {
            while ((rowObjects = reader.nextRow()) != null) {
                rowsInChunk++;
                recordsCount++;

                execSql = new StringBuilder(pattern);
                Integer length = table.getHeaders().size();
                for (int i = 0; i < rowObjects.size(); i++) {
                    execSql.append(getFieldData(headers.get(i), rowObjects.get(i)));
                    if (i < length - 1) {
                        execSql.append(",");
                    }
                }
                execSql.append("),");
                st.addBatch(prepareQuery(execSql.toString()));

                if (rowsInChunk == ROWS_IN_CHUNK) {
                    System.out.println("Готово.");
                    System.out.print("Вставляем chunk(" + ROWS_IN_CHUNK + ") в таблицу("
                            + (recordsCount - ROWS_IN_CHUNK) + ") '" + table.getTableName() + "'... ");
                    st.executeBatch();
                    rowsInChunk = 0;
                    System.out.println("Готово.");
                    System.out.print("Формируем chunk... ");
                }
            }
        }

        if (!execSql.toString().equals(pattern)) {
            System.out.println("Готово.");
            System.out.print("Вставляем chunk(" + rowsInChunk + ") в таблицу("
                    + (recordsCount - rowsInChunk) + ") '" + table.getTableName() + "'... ");
            st.executeBatch();
            System.out.println("Готово.");
        }
        System.out.println("Всего записей в таблице: " + recordsCount);
    }

    private String getFieldData(DbfTable.Header header, String object) throws IOException {
        switch (header.dataType) {
            case "NVARCHAR": {
                if (object == null) {
                    return null;
                }
                return "N'" + object + "'";
            }
            case "TEXT": {
                if (object == null) {
                    return null;
                }
                return "'" + object + "'";
            }
            case "DATE": {
                if (object == null) {
                    return null;
                }
                return "CONVERT(datetime, '" + object + "', 104)";
            }
            default:
                return object;
        }
    }

    private String getInsertPattern(DbfTable table) {
        return "INSERT INTO " + table.getTableName() + " VALUES(";
    }

    @Override
    String getCreateTablePattern(String tableName) {
        return "CREATE TABLE " + tableName + " (id INTEGER PRIMARY KEY not NULL IDENTITY(1,1),";
    }
}
