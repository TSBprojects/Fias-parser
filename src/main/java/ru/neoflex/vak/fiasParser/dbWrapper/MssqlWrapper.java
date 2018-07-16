package ru.neoflex.vak.fiasParser.dbWrapper;

import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.dbfApi.DbfTable;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MssqlWrapper extends DbWrapper implements AutoCloseable {


    public MssqlWrapper(MssqlProperties config, Callback onProgress) throws SQLException, ClassNotFoundException {
        super(config, onProgress);
    }

    @Override
    void fillDbfTable(DbfTable table) throws Exception {
        float currentTableStatus;
        Integer recordsCount = 0;
        Integer rowsInChunk = 0;
        String pattern = getInsertPattern(table);
        StringBuilder execSql = new StringBuilder("");
        ArrayList<DbfTable.Header> headers = table.getHeaders();

        Statement st = connection.createStatement();

        printStatus("Формируем chunk... ");
        ArrayList<String> rowObjects;
        try (DbfTable reader = table.startRead()) {
            while ((rowObjects = reader.nextRow()) != null) {
                if (!run) {
                    return;
                }

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
                    insertedRecordCount += rowsInChunk;

                    printDone("Готово.\n");
                    printStatus("Вставляем chunk(" + ROWS_IN_CHUNK + ") в таблицу("
                            + (recordsCount - ROWS_IN_CHUNK) + ") '" + table.getTableName() + "'... ");
                    st.executeBatch();
                    rowsInChunk = 0;
                    currentTableStatus = (float) recordsCount / table.getRecordCount();
                    printStatus("Готово.", currentTableStatus);
                    printStatus("Формируем chunk... ");
                }
            }
        }

        if (!execSql.toString().equals(pattern)) {
            insertedRecordCount += rowsInChunk;
            printDone("Готово.\n");
            printStatus("Вставляем chunk(" + rowsInChunk + ") в таблицу("
                    + (recordsCount - rowsInChunk) + ") '" + table.getTableName() + "'... ");
            st.executeBatch();
            currentTableStatus = (float) recordsCount / table.getRecordCount();
            printStatus("Готово.", currentTableStatus);
        }
        printStatus("Всего записей в таблице: " + recordsCount + "\n");
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
