package guide.reference.azure

import com.azure.core.credential.AzureKeyCredential
import com.azure.search.documents.indexes.SearchIndexClientBuilder
import org.http4k.azure.AzureHttpClient
import org.http4k.client.OkHttp
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters

fun main() {
    val http4kClient = DebuggingFilters.PrintRequestAndResponse().then(OkHttp())

    val searchIndexClient = SearchIndexClientBuilder()
        .endpoint("https://....")
        .credential(AzureKeyCredential("APIKEY"))
        .httpClient(AzureHttpClient(http4kClient))
        .buildClient()

    searchIndexClient.deleteIndex("myIndex")
}
