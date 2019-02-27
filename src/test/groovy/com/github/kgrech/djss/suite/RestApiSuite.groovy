package com.github.kgrech.djss.suite

import com.github.kgrech.djss.App
import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.TestCaseInitializer
import com.github.kgrech.djss.controller.AppControllerTest
import com.github.kgrech.djss.controller.TransferControllerTest
import org.junit.After
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
    AppControllerTest,
    TransferControllerTest
])
class RestApiSuite {

    private static App app

    @BeforeClass
    static void setup() {
        app = new TestCaseInitializer().withSpark().build()
        DBTest.setApp(app)
    }

    @After
    static void cleanup() {
        app.close()
    }
}
