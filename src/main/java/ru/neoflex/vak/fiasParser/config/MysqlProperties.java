package ru.neoflex.vak.fiasParser.config;

public class MysqlProperties extends DbProperties {
    public MysqlProperties(String verifyServerCertificate, String useSSL, String requireSSL, String useLegacyDatetimeCode, String serverTimezone, String hostName, String port, String databaseName, String user, String password) {
        this.verifyServerCertificate = verifyServerCertificate;
        this.useSSL = useSSL;
        this.requireSSL = requireSSL;
        this.useLegacyDatetimeCode = useLegacyDatetimeCode;
        if (serverTimezone.isEmpty()) {
            this.serverTimezone = "UTC";
        } else {
            this.serverTimezone = serverTimezone;
        }
        if (hostName.isEmpty()) {
            this.hostName = "localhost";
        } else {
            this.hostName = hostName;
        }
        if (port.isEmpty()) {
            this.port = "3306";
        } else {
            this.port = port;
        }
        this.databaseName = databaseName;
        if (user.isEmpty()) {
            this.user = "root";
        } else {
            this.user = user;
        }
        if (password.isEmpty()) {
            this.password = "root";
        } else {
            this.password = password;
        }
    }

    MysqlProperties() {
    }

    public String verifyServerCertificate;
    public String useSSL;
    public String requireSSL;
    public String useLegacyDatetimeCode;
    public String serverTimezone;
}
