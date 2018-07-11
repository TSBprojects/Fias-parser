package ru.neoflex.vak.fiasParser.dbWrapper;

import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.config.MysqlProperties;
import ru.neoflex.vak.fiasParser.fiasApi.DbfTable;
import ru.neoflex.vak.fiasParser.fiasApi.FiasDatabase;

import java.sql.*;
import java.util.ArrayList;

public abstract class DbWrapper implements AutoCloseable {

    static final int ROWS_IN_CHUNK = 1000;

    private Connection connection;


    DbWrapper(MssqlProperties config) {
        System.out.print("Соединение с БД... ");

        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            String connectionString = "jdbc:sqlserver://" + config.hostName + ":" + config.port +
                    ";databaseName=" + config.databaseName +
                    ";integratedSecurity=" + config.integratedSecurity +
                    ";user=" + config.user +
                    ";password=" + config.password;

            connection = DriverManager.getConnection(connectionString);

            System.out.println("Готово.\n");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    DbWrapper(MysqlProperties config) {
        System.out.print("Соединение с БД... ");

        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            String connectionString = "jdbc:mysql://" + config.hostName + ":" + config.port + "/" + config.databaseName +
                    "?verifyServerCertificate=" + config.verifyServerCertificate +
                    "&useSSL=" + config.useSSL +
                    "&requireSSL=" + config.requireSSL +
                    "&useLegacyDatetimeCode=" + config.useLegacyDatetimeCode +
                    "&amp" +
                    "&serverTimezone=" + config.serverTimezone;

            connection = DriverManager.getConnection(connectionString, config.user, config.password);

            System.out.println("Готово.\n");
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }


    public void copyTables(FiasDatabase db) throws Exception {
        ArrayList<DbfTable> tables = db.getTables();

        connection.setAutoCommit(false);
        System.out.println("Перенос данных в БД...\n");
        for (DbfTable table : tables) {
            createTableFromDbf(table);
        }
        connection.commit();
        System.out.println("Все данные перенесены в БД.");
    }


    String prepareQuery(String text) {
        return text.substring(0, text.length() - 1) + ";";
    }

    Statement createStatement() throws SQLException {
        return connection.createStatement();
    }

    void executeUpdate(String SQL) {
        try (PreparedStatement prepareStatement = connection.prepareStatement(SQL)) {
            prepareStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    abstract void fillDbfTable(DbfTable table) throws Exception;

    abstract String getCreateTablePattern(String tableName);


    private void createTableFromDbf(DbfTable table) throws Exception {
        String tableName = table.getTableName();
        if (isTableExist(tableName)) {
            dropTable(tableName);
        }

        System.out.print("Создание таблицы '" + tableName + "'... ");
        executeUpdate(getCreateTableSql(table));
        System.out.println("Готово.\n");

        System.out.println("Заполняем таблицу '" + tableName + "'... ");
        fillDbfTable(table);
        System.out.println("Таблица '" + tableName + "' заполнена.\n");
    }

    private void dropTable(String tableName) {
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


    public void close() {
        try {
            connection.close();
            connection = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
