package com.chromia.build.tools.test

import net.postchain.rell.base.runtime.Rt_Exception
import net.postchain.rell.base.runtime.utils.Rt_Utils
import net.postchain.rell.base.utils.UnitTestRunnerResults
import org.redundent.kotlin.xml.xml

fun UnitTestRunnerResults.xmlTestReport(): String =
        xml("testsuite") {
            attribute("name", "Rell tests")
            for (testCase in getResults()) {
                "testcase" {
                    attribute("classname", "Rell tests")
                    attribute("name", testCase.case.name)
                    if (!testCase.res.isOk) {
                        "failure" {
                            val message = testCase.res.error?.message ?: "FAILED"
                            attribute("message", message)
                            when (val exception = testCase.res.error) {
                                is Rt_Exception -> {
                                    -Rt_Utils.appendStackTrace(message, exception.info.stack)
                                }
                            }
                        }
                    }
                }
            }
        }.toString()
