package com.github.kgrech.djss


import liquibase.Liquibase
import liquibase.database.DatabaseFactory
import liquibase.database.jvm.JdbcConnection
import liquibase.resource.FileSystemResourceAccessor

class TestCaseInitializer {

    private App app = new App()
    private boolean withDB
    private boolean withProcessing
    private boolean withSpark

    TestCaseInitializer withDB() {
        this.withDB = true
        return this
    }

    TestCaseInitializer withProcessing() {
        withDB()
        withProcessing = true
        return this
    }

    TestCaseInitializer withSpark() {
        withDB()
        this.withSpark = true
        return this
    }

    App build() {
        app.initProperties('config.properties')
        if (withDB) {
            initDb()
        }
        if (withProcessing) {
            app.initProcessingService()
        }
        if (withSpark) {
            app.initSpark()
        }
        return app
    }

    private initDb() {
        app.initDSLContext()
        def provider = app.getProvider()
        def connection = provider.getConnection()

        def db = DatabaseFactory.getInstance()
                .findCorrectDatabaseImplementation(new JdbcConnection(connection))

        def liquibase = new Liquibase('liquibase/changelog.xml',
                new FileSystemResourceAccessor(), db)
        liquibase.dropAll()
        liquibase.update('test')
    }
}
