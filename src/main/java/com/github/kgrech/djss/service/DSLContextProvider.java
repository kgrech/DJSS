package com.github.kgrech.djss.service;

import com.github.kgrech.djss.App;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import org.apache.commons.dbcp.BasicDataSource;
import org.jooq.Configuration;
import org.jooq.ConnectionProvider;
import org.jooq.DSLContext;
import org.jooq.SQLDialect;
import org.jooq.impl.DSL;
import org.jooq.impl.DataSourceConnectionProvider;
import org.jooq.impl.DefaultConfiguration;
import org.jooq.impl.ThreadLocalTransactionProvider;

public class DSLContextProvider {

    private final BasicDataSource ds;

    public DSLContextProvider(Properties properties) {
        this.ds = new BasicDataSource();
        this.ds.setDriverClassName(properties.getProperty("db.driver"));
        this.ds.setUrl(properties.getProperty("db.url"));
        this.ds.setUsername(properties.getProperty("db.username"));
        this.ds.setPassword(properties.getProperty("db.password"));
    }

    public Connection getConnection() throws SQLException {
        return ds.getConnection();
    }

    public DSLContext getContext() {
        final ConnectionProvider cp = new DataSourceConnectionProvider(ds);
        final Configuration configuration = new DefaultConfiguration()
                .set(cp)
                .set(SQLDialect.H2)
                .set(new ThreadLocalTransactionProvider(cp, true));
        return DSL.using(configuration);
    }
}
