package org.http4k.format

import com.fasterxml.jackson.module.kotlin.KotlinModule

class JacksonYamlAutoTest : AutoMarshallingContract(JacksonYaml) {
    override val expectedAutoMarshallingResult: String = """string: "hello"
child:
  string: "world"
  child: null
  numbers:
  - 1
  bool: true
numbers: []
bool: false
"""
    override val expectedAutoMarshallingResultPrimitives: String = """duration: "PT1S"
localDate: "2000-01-01"
localTime: "01:01:01"
localDateTime: "2000-01-01T01:01:01"
zonedDateTime: "2000-01-01T01:01:01Z[UTC]"
offsetTime: "01:01:01Z"
offsetDateTime: "2000-01-01T01:01:01Z"
instant: "1970-01-01T00:00:00Z"
uuid: "1a448854-1687-4f90-9562-7d527d64383c"
uri: "http://uri:8000"
url: "http://url:9000"
status: 200
"""
    override val expectedWrappedMap: String = """value:
  key: "value"
  key2: "123"
"""

    override val expectedConvertToInputStream: String = """value: "hello"
"""
    override val expectedThrowable: String = """value: "org.http4k.format.CustomException: foobar"""
    override val inputUnknownValue: String = """value: "value"
unknown: "2000-01-01"        
"""
    override val inputEmptyObject: String = """"""
    override val expectedRegexSpecial: String = """regex: ".*"
"""
    override fun customMarshaller() = object : ConfigurableJacksonYaml(KotlinModule().asConfigurable().customise()) {}
}
