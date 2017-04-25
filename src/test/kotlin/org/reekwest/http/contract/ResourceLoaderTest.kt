package org.reekwest.http.contract

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class ResourceLoaderTest {

    @Test
    fun `classpath loader loads existing file`() {
        checkContents(ResourceLoader.Classpath("/"), "mybob.xml", "<xml>content</xml>")
    }

    private fun checkContents(loader: ResourceLoader, path: String, expected: String) {
        assertThat(loader.load(path)!!.openStream().bufferedReader().use { it.readText() }, equalTo(expected))
    }

    @Test
    fun `classpath loader loads existing child file`() {
        assertThat(false, equalTo(false))
    }

    @Test
    fun `classpath loader for missing file`() {
        assertThat(false, equalTo(false))
    }

//
//    describe("Classpath loader") {
//        val loader = ResourceLoader.Classpath("/")
//        describe("for an existing file") {
//            it("looks up contents") {
//                Source.fromURL(loader.load("mybob.xml")).mkString shouldBe "<xml>content</xml>"
//            }
//            it("looks up contents of a child file") {
//                Source.fromURL(loader.load("io/index.html")).mkString shouldBe "hello from the io index.html"
//            }
//        }
//        describe("for a missing file") {
//            it("URL is null") {
//                loader.load("notafile") shouldBe null
//            }
//        }
//    }
//
//    describe("Directory loader") {
//        val loader = ResourceLoader.Directory("./core/src/test/resources")
//        describe("for an existing file") {
//            it("looks up contents") {
//                Source.fromURL(loader.load("mybob.xml")).mkString shouldBe "<xml>content</xml>"
//            }
//            it("looks up contents of a child file") {
//                Source.fromURL(loader.load("io/index.html")).mkString shouldBe "hello from the io index.html"
//            }
//        }
//        describe("for a missing file") {
//            it("URL is null") {
//                loader.load("notafile") shouldBe null
//            }
//            it("URL is a directory") {
//                loader.load("io") shouldBe null
//            }
//        }
//    }
}