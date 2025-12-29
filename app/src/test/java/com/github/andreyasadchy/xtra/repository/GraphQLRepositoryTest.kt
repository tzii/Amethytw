 package com.github.andreyasadchy.xtra.repository
 
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.ExperimentalCoroutinesApi
 import kotlinx.coroutines.test.StandardTestDispatcher
 import kotlinx.coroutines.test.resetMain
 import kotlinx.coroutines.test.runTest
 import kotlinx.coroutines.test.setMain
 import kotlinx.serialization.json.Json
 import okhttp3.OkHttpClient
 import org.junit.After
 import org.junit.Assert.*
 import org.junit.Before
 import org.junit.Test
 import org.mockito.kotlin.mock
 import java.util.concurrent.ExecutorService
 
 @OptIn(ExperimentalCoroutinesApi::class)
 class GraphQLRepositoryTest {
 
     private val testDispatcher = StandardTestDispatcher()
     private lateinit var okHttpClient: OkHttpClient
     private lateinit var json: Json
     private lateinit var cronetExecutor: ExecutorService
     private lateinit var repository: GraphQLRepository
 
     @Before
     fun setup() {
         Dispatchers.setMain(testDispatcher)
         okHttpClient = mock()
         json = Json { ignoreUnknownKeys = true }
         cronetExecutor = mock()
         repository = GraphQLRepository(
             httpEngine = null,
             cronetEngine = null,
             cronetExecutor = cronetExecutor,
             okHttpClient = okHttpClient,
             json = json
         )
     }
 
     @After
     fun tearDown() {
         Dispatchers.resetMain()
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody builds correct body for stream`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "teststreamer",
             vodId = null,
             playerType = "embed"
         )
         
         assertTrue(body.contains("\"isLive\":true"))
         assertTrue(body.contains("\"login\":\"teststreamer\""))
         assertTrue(body.contains("\"isVod\":false"))
         assertTrue(body.contains("\"vodID\":\"\""))
         assertTrue(body.contains("\"playerType\":\"embed\""))
         assertTrue(body.contains("\"platform\":\"web\""))
         assertTrue(body.contains("PlaybackAccessToken"))
         assertTrue(body.contains("ed230aa1e33e07eebb8928504583da78a5173989fadfb1ac94be06a04f3cdbe9"))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody builds correct body for VOD`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = null,
             vodId = "1234567890",
             playerType = "site"
         )
         
         assertTrue(body.contains("\"isLive\":false"))
         assertTrue(body.contains("\"login\":\"\""))
         assertTrue(body.contains("\"isVod\":true"))
         assertTrue(body.contains("\"vodID\":\"1234567890\""))
         assertTrue(body.contains("\"playerType\":\"site\""))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody handles null playerType`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "teststreamer",
             vodId = null,
             playerType = null
         )
         
         assertTrue(body.contains("\"playerType\":null"))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody handles both null login and vodId`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = null,
             vodId = null,
             playerType = "embed"
         )
         
         assertTrue(body.contains("\"isLive\":false"))
         assertTrue(body.contains("\"isVod\":false"))
         assertTrue(body.contains("\"login\":\"\""))
         assertTrue(body.contains("\"vodID\":\"\""))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody handles empty login`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "",
             vodId = null,
             playerType = "embed"
         )
         
         assertTrue(body.contains("\"isLive\":false"))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody handles blank login`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "   ",
             vodId = null,
             playerType = "embed"
         )
         
         assertTrue(body.contains("\"isLive\":false"))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody contains persisted query hash`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "test",
             vodId = null,
             playerType = "embed"
         )
         
         assertTrue(body.contains("persistedQuery"))
         assertTrue(body.contains("sha256Hash"))
         assertTrue(body.contains("version"))
     }
 
     @Test
     fun `getPlaybackAccessTokenRequestBody prioritizes stream over VOD when both provided`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "streamer",
             vodId = "12345",
             playerType = "embed"
         )
         
         assertTrue(body.contains("\"isLive\":true"))
         assertTrue(body.contains("\"isVod\":true"))
         assertTrue(body.contains("\"login\":\"streamer\""))
         assertTrue(body.contains("\"vodID\":\"12345\""))
     }
 
     @Test
     fun `JSON body is valid JSON format`() {
         val body = repository.getPlaybackAccessTokenRequestBody(
             login = "test",
             vodId = null,
             playerType = "embed"
         )
         
         assertDoesNotThrow {
             json.parseToJsonElement(body)
         }
     }
 
     private fun assertDoesNotThrow(block: () -> Unit) {
         try {
             block()
         } catch (e: Exception) {
             fail("Expected no exception but got: ${e.message}")
         }
     }
 }
