package ru.neoflex.vak.fiasParser.config;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ParsConfig {

    private DbType dbType;
    private MysqlProperties mysqlProp;
    private MssqlProperties mssqlProp;
    private String fiasDirPath;

    public DbType getDbType() {
        return dbType;
    }

    public MysqlProperties getMysqlProp() {
        return mysqlProp;
    }

    public MssqlProperties getMssqlProp() {
        return mssqlProp;
    }

    public String getFiasDirPath() {
        return fiasDirPath;
    }

    public ParsConfig(String dbType, MysqlProperties mysqlProp, MssqlProperties mssqlProp, String fiasDirPath) {
        if (dbType.equals("mssql")) {
            this.dbType = DbType.mssql;
        } else {
            this.dbType = DbType.mysql;
        }
        this.mysqlProp = mysqlProp;
        this.mssqlProp = mssqlProp;
        this.fiasDirPath = fiasDirPath;
    }

    public ParsConfig(String configPath) throws IOException {
        mysqlProp = new MysqlProperties();
        mssqlProp = new MssqlProperties();
        readConfig(Paths.get(configPath));
    }

    private void readConfig(Path configPath) throws IOException {
        if (Files.exists(configPath)) {
            List<String> props = Files.readAllLines(configPath);
            for (String propLine : props) {
                if (!propLine.isEmpty() && propLine.charAt(0) != '[' && propLine.charAt(0) != '#') {
                    setProp(propLine);
                }
            }
        } else {
            Files.createFile(Paths.get("config.ini"));
            Files.write(
                    Paths.get("config.ini"),
                    Collections.singletonList(
                            "[Database type]\n" +
                                    "# mssql/mysql\n" +
                                    "db_type_name=\n\n" +
                                    "[Database connection]\n" +
                                    "# If properties not specified,\n" +
                                    "# will be used the default values for\n" +
                                    "# -- host_name=localhost\n" +
                                    "# -- port\n" +
                                    "database_name=\n" +
                                    "host_name=\n" +
                                    "port=\n" +
                                    "user=\n" +
                                    "password=\n\n" +
                                    "[MySql connection]\n" +
                                    "# the default will be used for\n" +
                                    "# -- port=3306\n" +
                                    "verify_server_certificate=false\n" +
                                    "use_SSL=false\n" +
                                    "require_SSL=false\n" +
                                    "use_legacyDatetime_code=false\n" +
                                    "server_timezone=UTC\n\n" +
                                    "[MsSql connection]\n" +
                                    "# the default will be used for\n" +
                                    "# -- port=1433\n" +
                                    "# -- integrated_security=true\n" +
                                    "integrated_security=true\n\n" +
                                    "[FIAS files path]\n" +
                                    "fias_dir_path="),
                    Charset.forName("UTF-8"));
            throw new IOException("Отсутсвовал файл инициализации. Файл создан, заполните его.");
        }
    }

    private void setProp(String propLine) throws IOException {
        Pattern p = Pattern.compile("(.+)=(.{0,})");
        Matcher m = p.matcher(propLine);
        if (m.matches()) {
            switch (m.group(1)) {
                case "db_type_name": {
                    if (m.group(2).isEmpty()) {
                        throw new IOException("Не указан тип базы данных!");
                    }
                    switch (m.group(2)) {
                        case "mssql": {
                            dbType = DbType.mssql;
                            break;
                        }
                        case "mysql": {
                            dbType = DbType.mysql;
                            break;
                        }
                    }
                    break;
                }
                case "database_name": {
                    if (m.group(2).isEmpty()) {
                        throw new IOException("Не указано имя базы данных!");
                    }
                    mysqlProp.databaseName = m.group(2);
                    mssqlProp.databaseName = m.group(2);
                    break;
                }
                case "host_name": {
                    String host = m.group(2);
                    if (host.isEmpty()) {
                        host = "localhost";
                    }
                    mysqlProp.hostName = host;
                    mssqlProp.hostName = host;
                    break;
                }
                case "port": {
                    String port = m.group(2);
                    if (port.isEmpty()) {
                        mysqlProp.port = "3306";
                        mssqlProp.port = "1433";
                    } else {
                        mysqlProp.port = port;
                        mssqlProp.port = port;
                    }
                    break;
                }
                case "user": {
                    mysqlProp.user = m.group(2);
                    mssqlProp.user = m.group(2);
                    break;
                }
                case "password": {
                    mysqlProp.password = m.group(2);
                    mssqlProp.password = m.group(2);
                    break;
                }
                case "integrated_security": {
                    String integratedSecurity = m.group(2);
                    if (integratedSecurity.isEmpty()) {
                        integratedSecurity = "true";
                    }
                    mssqlProp.integratedSecurity = integratedSecurity;
                    break;
                }
                case "verify_server_certificate": {
                    mysqlProp.verifyServerCertificate = m.group(2);
                    break;
                }
                case "use_SSL": {
                    mysqlProp.useSSL = m.group(2);
                    break;
                }
                case "require_SSL": {
                    mysqlProp.requireSSL = m.group(2);
                    break;
                }
                case "use_legacyDatetime_code": {
                    mysqlProp.useLegacyDatetimeCode = m.group(2);
                    break;
                }
                case "server_timezone": {
                    mysqlProp.serverTimezone = m.group(2);
                    break;
                }
                case "fias_dir_path": {
                    fiasDirPath = m.group(2);
                    if (m.group(2).isEmpty()) {
                        throw new IOException("Не указана директория с dbf файлами!");
                    }
                    break;
                }
                default:
                    throw new IOException("Неизвестное свойство!");
            }

        } else {
            throw new IOException("Неверная сигнатура свойства!");
        }
    }

}
