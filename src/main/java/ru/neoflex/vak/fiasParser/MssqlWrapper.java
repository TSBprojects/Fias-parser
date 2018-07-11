package ru.neoflex.vak.fiasParser;

import ru.neoflex.vak.fiasParser.config.MssqlProperties;
import ru.neoflex.vak.fiasParser.fiasApi.DbfTable;

import java.io.IOException;

public class MssqlWrapper extends DbWrapper implements AutoCloseable {

    MssqlWrapper(MssqlProperties config) {
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
                return "CONVERT(datetime, '" + object + "', 104)";
            }
            default:
                return object;
        }
    }

    @Override
    public String getInsertPattern(DbfTable table) {
        return "INSERT INTO " + table.getTableName() + " VALUES(";
    }

    @Override
    public String getCreateTablePattern(String tableName) {
        return "CREATE TABLE " + tableName + " (id INTEGER PRIMARY KEY not NULL IDENTITY(1,1),";
    }
}
