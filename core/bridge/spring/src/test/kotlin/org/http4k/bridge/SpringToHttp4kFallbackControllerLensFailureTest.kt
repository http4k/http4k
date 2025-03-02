package org.http4k.bridge

import org.http4k.lens.Path
import org.http4k.lens.int
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.net.URI

class SpringToHttp4kFallbackControllerLensFailureTest {
    val target = object {}
    
    val p = Path.int().of("p")
    
    inner class Controller : SpringToHttp4kFallbackController({ rq ->
        p(rq)
        error("should have thrown LensFailure")
    })
    
    val mvc = MockMvcBuilders.standaloneSetup(Controller()).build()
    
    @Test
    fun `catches LensFailure and returns 400`() {
        mvc.get(URI("/")) {}
            .andExpect { status { isBadRequest() } }
            .andExpect { content { contentTypeCompatibleWith(APPLICATION_PROBLEM_JSON) } }
    }
}
