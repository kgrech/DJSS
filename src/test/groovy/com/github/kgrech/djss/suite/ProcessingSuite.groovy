package com.github.kgrech.djss.suite

import com.github.kgrech.djss.App
import com.github.kgrech.djss.DBTest
import com.github.kgrech.djss.TestCaseInitializer
import com.github.kgrech.djss.service.processing.TransferDispensingRunnableTest
import com.github.kgrech.djss.service.processing.TransferProcessingRunnableLogicTest
import com.github.kgrech.djss.service.processing.TransferProcessingRunnableNegativeTest
import org.junit.After
import org.junit.BeforeClass
import org.junit.runner.RunWith
import org.junit.runners.Suite

@RunWith(Suite)
@Suite.SuiteClasses([
    TransferDispensingRunnableTest,
    TransferProcessingRunnableLogicTest,
    TransferProcessingRunnableNegativeTest
])
class ProcessingSuite {

    private static App app

    @BeforeClass
    static void setup() {
        app = new TestCaseInitializer().withDB().build()
        DBTest.setApp(app)
    }

    @After
    static void cleanup() {
        app.close()
    }
}