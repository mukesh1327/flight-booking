package com.cloudxplorer.authservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    private final Db db = new Db();
    private final Rhbk rhbk = new Rhbk();

    public Db getDb() {
        return db;
    }

    public Rhbk getRhbk() {
        return rhbk;
    }

    public static class Db {
        private String url;
        private String username;
        private String password;
        private String healthTable = "public.flightapp";
        private String userTable = "public.flightapp";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getHealthTable() {
            return healthTable;
        }

        public void setHealthTable(String healthTable) {
            this.healthTable = healthTable;
        }

        public String getUserTable() {
            return userTable;
        }

        public void setUserTable(String userTable) {
            this.userTable = userTable;
        }
    }

    public static class Rhbk {
        private String healthUrl = "http://localhost:8090/realms/authservice/.well-known/openid-configuration";
        private String tokenUrl = "http://localhost:8090/realms/authservice/protocol/openid-connect/token";
        private String userInfoUrl = "http://localhost:8090/realms/authservice/protocol/openid-connect/userinfo";
        private String logoutUrl = "http://localhost:8090/realms/authservice/protocol/openid-connect/logout";
        private String usersUrl = "http://localhost:8090/admin/realms/authservice/users";
        private String adminTokenUrl = "http://localhost:8090/realms/authservice/protocol/openid-connect/token";
        private String clientId = "authservice-client";
        private String clientSecret = "jXvZoiPpvazLf8VUl2qxbZHjBj66FSVN";
        private String adminClientId = "authservice-admin";
        private String adminClientSecret = "";
        private int timeoutMs = 3000;

        public String getHealthUrl() {
            return healthUrl;
        }

        public void setHealthUrl(String healthUrl) {
            this.healthUrl = healthUrl;
        }

        public String getTokenUrl() {
            return tokenUrl;
        }

        public void setTokenUrl(String tokenUrl) {
            this.tokenUrl = tokenUrl;
        }

        public String getUserInfoUrl() {
            return userInfoUrl;
        }

        public void setUserInfoUrl(String userInfoUrl) {
            this.userInfoUrl = userInfoUrl;
        }

        public String getLogoutUrl() {
            return logoutUrl;
        }

        public void setLogoutUrl(String logoutUrl) {
            this.logoutUrl = logoutUrl;
        }

        public String getUsersUrl() {
            return usersUrl;
        }

        public void setUsersUrl(String usersUrl) {
            this.usersUrl = usersUrl;
        }

        public String getAdminTokenUrl() {
            return adminTokenUrl;
        }

        public void setAdminTokenUrl(String adminTokenUrl) {
            this.adminTokenUrl = adminTokenUrl;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getAdminClientId() {
            return adminClientId;
        }

        public void setAdminClientId(String adminClientId) {
            this.adminClientId = adminClientId;
        }

        public String getAdminClientSecret() {
            return adminClientSecret;
        }

        public void setAdminClientSecret(String adminClientSecret) {
            this.adminClientSecret = adminClientSecret;
        }

        public int getTimeoutMs() {
            return timeoutMs;
        }

        public void setTimeoutMs(int timeoutMs) {
            this.timeoutMs = timeoutMs;
        }
    }
}
