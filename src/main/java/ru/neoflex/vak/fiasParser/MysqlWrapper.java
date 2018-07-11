package ru.neoflex.vak.fiasParser;

import ru.neoflex.vak.fiasParser.config.MysqlProperties;
import ru.neoflex.vak.fiasParser.fiasApi.DbfTable;

import java.io.IOException;
import java.util.ArrayList;

public class MysqlWrapper extends DbWrapper implements AutoCloseable {

    MysqlWrapper(MysqlProperties config) {
        super(config);
    }

    @Override
    public String getFieldData(DbfTable.Header header, String object) throws IOException {
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

    @Override
    public String getInsertPattern(DbfTable table) {
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
        return "INSERT INTO " + table.getTableName() + " " + insertHeaders + " VALUES(";
    }

    @Override
    public String getCreateTablePattern(String tableName) {
        return "CREATE TABLE " + tableName + " (id INTEGER PRIMARY KEY not NULL AUTO_INCREMENT,";
    }
}
