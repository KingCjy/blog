package com.kingcjy.replication.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Component
@ConfigurationProperties("datasource")
public class DatabaseProperty {
    private String url;
    private List<Slave> slaveList;

    private String username;
    private String password;

    @Getter
    @Setter
    public static class Slave {
        private String name;
        private String url;
    }
}
