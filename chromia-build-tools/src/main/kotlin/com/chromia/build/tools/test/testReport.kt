package com.chromia.build.tools.test

import net.postchain.rell.base.runtime.Rt_Exception
import net.postchain.rell.base.runtime.utils.Rt_Utils
import net.postchain.rell.base.utils.UnitTestRunnerResults
import org.redundent.kotlin.xml.xml

fun UnitTestRunnerResults.xmlTestReport(name: String): String =
        xml("testsuite") {
            val results = getResults()
            attribute("name", name)
            attribute("failures", results.filter { !it.res.isOk }.size)
            attribute("tests", results.size)
            attribute("version", "3.0") // xml schema version
            for (testCase in results) {
                "testcase" {
                    attribute("classname", testCase.case.fn.defName.module)
                    attribute("name", testCase.case.fn.simpleName)
                    if (!testCase.res.isOk) {
                        "failure" {
                            val message = testCase.res.error?.message ?: "FAILED"
                            attribute("message", message)
                            when (val exception = testCase.res.error) {
                                is Rt_Exception -> {
                                    cdata(Rt_Utils.appendStackTrace(message, exception.info.stack))
                                }
                            }
                        }
                    }
                }
            }
        }.toString()
