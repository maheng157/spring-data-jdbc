package io.github.mado.jdbc.datasource.support;

import com.zaxxer.hikari.HikariConfig;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;
import java.util.Map;

/**
 * @author heng.ma
 */
@ConfigurationProperties(prefix = "spring.dynamic")
public class MultipleDataSourceProperties {

    private Map<String, List<Properties>> datasource;


    public Map<String, List<Properties>> getDatasource() {
        return datasource;
    }

    public void setDatasource(Map<String, List<Properties>> datasource) {
        this.datasource = datasource;
    }

    public static class Properties extends DataSourceProperties {
        private HikariConfig hikari;

        public HikariConfig getHikari() {
            return hikari;
        }

        public void setHikari(HikariConfig hikari) {
            this.hikari = hikari;
        }
    }
}