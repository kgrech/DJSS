package com.github.kgrech.djss.suite

import com.github.kgrech.djss.App
import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.TestCaseInitializer
import com.github.kgrech.djss.end2end.End2EndTest
import org.junit.After
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
        End2EndTest.class
])
class End2EndSuite {

    private static App app

    @BeforeClass
    static void setup() {
        app = new TestCaseInitializer()
                .withSpark()
                .withProcessing()
                .build()
        DBTest.setApp(app)
    }

    @After
    static void cleanup() {
        app.close()
    }
}
