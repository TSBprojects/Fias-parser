package ru.neoflex.vak.fiasParser.dbWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.neoflex.vak.fiasParser.config.MysqlProperties;
import ru.neoflex.vak.fiasParser.dbfApi.DbfTable;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;

public class MysqlWrapper extends DbWrapper implements AutoCloseable {

    private final Logger log = LogManager.getLogger(MysqlWrapper.class.getName());

    public MysqlWrapper(MysqlProperties config, Callback onProgress) throws SQLException, ClassNotFoundException {
        super(config, onProgress);
    }

    @Override
    void fillDbfTable(DbfTable table) throws Exception {
        String query;
        float currentTableStatus;
        Integer recordsCount = 0;
        Integer rowsInChunk = 0;
        String pattern = getInsertPattern(table);
        StringBuilder execSql = new StringBuilder(pattern);
        ArrayList<DbfTable.Header> headers = table.getHeaders();

        printStatus("Формируем chunk... ");
        ArrayList<String> rowObjects;
        try (DbfTable reader = table.startRead()) {
            while ((rowObjects = reader.nextRow()) != null) {
                if (!run) {
                    return;
                }

                rowsInChunk++;
                recordsCount++;

                execSql.append("(");
                Integer length = table.getHeaders().size();
                for (int i = 0; i < rowObjects.size(); i++) {
                    execSql.append(getFieldData(headers.get(i), rowObjects.get(i)));
                    if (i < length - 1) {
                        execSql.append(",");
                    }
                }
                execSql.append("),");

                if (rowsInChunk == ROWS_IN_CHUNK) {
                    insertedRecordCount += rowsInChunk;

                    printDone("Готово.\n");
                    printStatus("Вставляем chunk(" + ROWS_IN_CHUNK + ") в таблицу("
                            + (recordsCount - ROWS_IN_CHUNK) + ") '" + table.getTableName() + "'... ");
                    query = prepareQuery(execSql.toString());
                    if (log.isDebugEnabled()) {
                        log.debug("(mysql) Executing batch. (SQL: " + query + ")");
                    } else {
                        log.info("(mysql) Executing batch.");
                    }
                    executeBatch(query);
                    execSql = new StringBuilder(pattern);
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
            query = prepareQuery(execSql.toString());
            if (log.isDebugEnabled()) {
                log.debug("(mysql) Executing batch. (SQL: " + query + ")");
            } else {
                log.info("(mysql) Executing batch.");
            }
            executeBatch(query);
            currentTableStatus = (float) recordsCount / table.getRecordCount();
            printStatus("Готово.", currentTableStatus);
        }
        printStatus("Всего записей в таблице: " + recordsCount + "\n");
    }

    private void executeBatch(String query) throws Exception {
        try {
            executeUpdate(query);
        } catch (Exception e) {
            log.warn("(mssql) Last chunk: " + query);
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
                return "STR_TO_DATE('" + object + "','%d.%m.%Y')";
            }
            default:
                return object;
        }
    }

    private String getInsertPattern(DbfTable table) {
        StringBuilder insertHeaders = new StringBuilder("(");
        ArrayList<DbfTable.Header> headers = table.getHeaders();

        Integer length = headers.size();
        for (int i = 0; i < length; i++) {
            insertHeaders.append(headers.get(i).name);
            if (i < length - 1) {
                insertHeaders.append(",");
            }
        }
        insertHeaders.append(")");
        return "INSERT INTO " + table.getTableName() + " " + insertHeaders + " VALUES";
    }

    @Override
    String getCreateTablePattern(String tableName) {
        return "CREATE TABLE " + tableName + " (id INTEGER PRIMARY KEY not NULL AUTO_INCREMENT,";
    }
}
