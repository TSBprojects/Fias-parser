package ru.neoflex.vak.fiasParser;

import ru.neoflex.vak.fiasParser.config.ParsConfig;
import ru.neoflex.vak.fiasParser.dbWrapper.DbWrapper;
import ru.neoflex.vak.fiasParser.dbWrapper.MssqlWrapper;
import ru.neoflex.vak.fiasParser.dbWrapper.MysqlWrapper;
import ru.neoflex.vak.fiasParser.dbfApi.DbfDatabase;

import java.nio.file.Paths;

public class DbfParser {
    private DbfParser() {
    }

    public static void start(ParsConfig config, DbWrapper.Callback onProgress) throws Exception {
        switch (config.getDbType()) {
            case mssql: {
                try (DbWrapper db = new MssqlWrapper(config.getMssqlProp(),onProgress)) {
                    db.copyTables(new DbfDatabase(Paths.get(config.getFiasDirPath())));
                }
                break;
            }
            case mysql: {
                try (DbWrapper db = new MysqlWrapper(config.getMysqlProp(),onProgress)) {
                    db.copyTables(new DbfDatabase(Paths.get(config.getFiasDirPath())));
                }
                break;
            }
        }
    }
}
