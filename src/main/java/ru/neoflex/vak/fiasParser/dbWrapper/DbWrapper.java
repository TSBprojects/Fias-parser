package ru.neoflex.vak.fiasParser.dbWrapper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.config.MysqlProperties;
import ru.neoflex.vak.fiasParser.dbfApi.DbfDatabase;
import ru.neoflex.vak.fiasParser.dbfApi.DbfTable;

import java.math.RoundingMode;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.ArrayList;

public abstract class DbWrapper implements AutoCloseable {

    private final Logger log = LogManager.getLogger(DbWrapper.class.getName());

    private Callback callback;

    private Integer totalRecordCount;

    static final int ROWS_IN_CHUNK = 1000;

    Connection connection;

    Integer insertedRecordCount = 0;

    boolean run = true;


    DbWrapper(MssqlProperties config, Callback onProgress) throws ClassNotFoundException, SQLException {
        this.callback = onProgress;
        log.info("Connecting to the database.");
        printStatus("Соединение с БД... ");

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        String connectionString = "jdbc:sqlserver://" + config.hostName + ":" + config.port +
                ";databaseName=" + config.databaseName +
                ";integratedSecurity=" + config.integratedSecurity +
                ";user=" + config.user +
                ";password=" + config.password;

        connection = DriverManager.getConnection(connectionString);

        printDone("Готово.\n\n");
    }

    DbWrapper(MysqlProperties config, Callback onProgress) throws ClassNotFoundException, SQLException {
        this.callback = onProgress;
        log.info("Connecting to the database.");
        printStatus("Соединение с БД... ");

        Class.forName("com.mysql.cj.jdbc.Driver");
        String connectionString = "jdbc:mysql://" + config.hostName + ":" + config.port + "/" + config.databaseName +
                "?verifyServerCertificate=" + config.verifyServerCertificate +
                "&useSSL=" + config.useSSL +
                "&requireSSL=" + config.requireSSL +
                "&useLegacyDatetimeCode=" + config.useLegacyDatetimeCode +
                "&amp" +
                "&serverTimezone=" + config.serverTimezone;

        connection = DriverManager.getConnection(connectionString, config.user, config.password);

        printDone("Готово.\n\n");
    }

    public interface Callback {
        boolean onProgress(String message, float fullProgress, float currentProgress);
    }

    public void copyTables(DbfDatabase db) throws Exception {
        ArrayList<DbfTable> tables = db.getTables();
        totalRecordCount = db.getRecordCount();

        connection.setAutoCommit(false);
        log.info("Start transferring data to database...");
        printStatus("Перенос данных в БД...\n\n");
        for (DbfTable table : tables) {
            createTableFromDbf(table);
        }
        connection.commit();
        log.info("Commit changes.");
        printStatus("Все данные перенесены в БД.\n");
        insertedRecordCount = 0;
    }


    String prepareQuery(String text) {
        return text.substring(0, text.length() - 1) + ";";
    }

    void executeUpdate(String SQL) throws SQLException {
        try (PreparedStatement prepareStatement = connection.prepareStatement(SQL)) {
            prepareStatement.executeUpdate();
        }
    }

    abstract void fillDbfTable(DbfTable table) throws Exception;

    abstract String getCreateTablePattern(String tableName);


    private void createTableFromDbf(DbfTable table) throws Exception {
        String query;
        String tableName = table.getTableName();
        if (isTableExist(tableName)) {
            dropTable(tableName);
        }

        query = getCreateTableSql(table);
        log.info("Creating a table '" + tableName + "'(SQL: " + query + ").");
        printStatus("Создание таблицы '" + tableName + "'... ");
        executeUpdate(query);
        printDone("Готово.\n");

        log.info("Filling the table '" + tableName + "'.");
        printStatus("Заполняем таблицу '" + tableName + "'... \n");
        fillDbfTable(table);
        printStatus("Таблица '" + tableName + "' заполнена.\n\n");
    }

    private void dropTable(String tableName) throws SQLException {
        log.info("Delete table '" + tableName + "'.");
        executeUpdate("DROP TABLE " + tableName);
    }

    private String getCreateTableSql(DbfTable table) {
        String dataTypeSize;
        DbfTable.Header header;
        Integer length = table.getHeaders().size();
        StringBuilder createTableSql = new StringBuilder(getCreateTablePattern(table.getTableName()));

        for (int i = 0; i < length; i++) {
            dataTypeSize = "";
            header = table.getHeaders().get(i);
            if (!header.dataTypeSize.isEmpty()) {
                dataTypeSize = "(" + header.dataTypeSize + ")";
            }

            createTableSql.append(String.format(
                    " %1$s %2$s%3$s",
                    header.name,
                    header.dataType,
                    dataTypeSize));

            if (i < length - 1) {
                createTableSql.append(",");
            }
        }
        createTableSql.append(")");
        return createTableSql.toString();
    }

    private boolean isTableExist(String tableName) throws SQLException {
        DatabaseMetaData metadata = connection.getMetaData();
        ResultSet resultSet;
        resultSet = metadata.getTables(null, null, tableName, null);
        return resultSet.next();
    }

    private String prepareStatus(float progress) {
        DecimalFormat df = new DecimalFormat("#.#");
        df.setRoundingMode(RoundingMode.CEILING);
        return df.format(progress);
    }

    void printStatus(String text, float currentTableProgress) {
///        try {
//            new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
//        } catch (InterruptedException | IOException e) {
//            e.printStackTrace();
//        }
        float fullProgress = (float) insertedRecordCount / totalRecordCount;

        if (callback != null) {
            run = callback.onProgress(text, fullProgress, currentTableProgress);
        } else {
            System.out.print(text + "[all:" + prepareStatus(100 * fullProgress) +
                    "%, cur:" + prepareStatus(100 * currentTableProgress) + "%]\n");
        }
    }

    void printStatus(String text) {
        if (callback != null) {
            run = callback.onProgress(text, -1, -1);
        } else {
            System.out.print(text);
        }
    }

    void printDone(String done) {
        if (callback == null) {
            System.out.print(done);
        }
    }

    public void close() throws SQLException {
        connection.close();
        connection = null;
    }
}
