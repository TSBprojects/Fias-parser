package ru.neoflex.vak.fiasParser.config;

public class MssqlProperties extends DbProperties {
    public MssqlProperties(String integratedSecurity, String hostName, String port, String databaseName, String user, String password) {
        this.integratedSecurity = integratedSecurity;
        if (hostName.isEmpty()) {
            this.hostName = "localhost";
        } else {
            this.hostName = hostName;
        }
        if (port.isEmpty()) {
            this.port = "1433";
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

    MssqlProperties() {
    }

    public String integratedSecurity;
}
