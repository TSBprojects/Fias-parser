package ru.neoflex.vak.fiasParser.dbWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.dbfApi.DbfTable;

import java.io.IOException;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MssqlWrapper extends DbWrapper implements AutoCloseable {

    private final Logger log = LogManager.getLogger(MssqlWrapper.class.getName());

    public MssqlWrapper(MssqlProperties config, Callback onProgress) throws SQLException, ClassNotFoundException {
        super(config, onProgress);
    }

    @Override
    void fillDbfTable(DbfTable table) throws Exception {
        float currentTableStatus;
        Integer recordsCount = 0;
        Integer rowsInChunk = 0;
        String pattern = getInsertPattern(table);
        StringBuilder chunk = new StringBuilder("");
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
                execSql.append(");");
                chunk.append(execSql.toString());

                if (log.isDebugEnabled()) {
                    log.debug("(mssql) Adding a command to the SQL batch: " + execSql.toString());
                }
                st.addBatch(execSql.toString());

                if (rowsInChunk == ROWS_IN_CHUNK) {
                    insertedRecordCount += rowsInChunk;

                    printDone("Готово.\n");
                    printStatus("Вставляем chunk(" + ROWS_IN_CHUNK + ") в таблицу("
                            + (recordsCount - ROWS_IN_CHUNK) + ") '" + table.getTableName() + "'... ");
                    log.info("(mssql) Executing batch.");
                    executeBatch(st, chunk.toString());
                    rowsInChunk = 0;
                    chunk = new StringBuilder("");
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
            log.info("(mssql) Executing batch.");
            executeBatch(st, chunk.toString());
            currentTableStatus = (float) recordsCount / table.getRecordCount();
            printStatus("Готово.", currentTableStatus);
        }
        printStatus("Всего записей в таблице: " + recordsCount + "\n");
    }

    private void executeBatch(Statement st, String lastChunk) throws Exception {
        try {
            st.executeBatch();
        } catch (Exception e) {
            log.warn("(mssql) Last chunk: " + lastChunk);
            throw new Exception(e);
        }
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
