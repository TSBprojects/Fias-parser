package ru.neoflex.vak.fiasParser;

import ru.neoflex.vak.fiasParser.config.ParsConfig;
import ru.neoflex.vak.fiasParser.fiasApi.FiasDatabase;

import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {

        //ParsConfig config = new ParsConfig("D:\\Documents\\IdeaProjects\\Fias parser\\Packaged application\\config.ini");
        ParsConfig config = new ParsConfig(args[0]);

        switch (config.getDbType()) {
            case mssql: {
                try (DbWrapper db = new MssqlWrapper(config.getMssqlProp())) {
                    db.copyTables(new FiasDatabase(Paths.get(config.getFiasDirPath())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
            case mysql: {
                try (DbWrapper db = new MysqlWrapper(config.getMysqlProp())) {
                    db.copyTables(new FiasDatabase(Paths.get(config.getFiasDirPath())));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                break;
            }
        }

    }
}