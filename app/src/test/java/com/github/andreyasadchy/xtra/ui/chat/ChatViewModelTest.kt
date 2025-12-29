 package com.github.andreyasadchy.xtra.ui.chat
 
 import android.content.Context
 import com.github.andreyasadchy.xtra.model.chat.ChatMessage
 import com.github.andreyasadchy.xtra.model.chat.Emote
 import com.github.andreyasadchy.xtra.model.chat.TwitchBadge
 import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
 import com.github.andreyasadchy.xtra.repository.EmoteCache
 import com.github.andreyasadchy.xtra.repository.GraphQLRepository
 import com.github.andreyasadchy.xtra.repository.HelixRepository
 import com.github.andreyasadchy.xtra.repository.PlayerRepository
 import com.github.andreyasadchy.xtra.repository.TranslateAllMessagesUsersRepository
 import kotlinx.coroutines.Dispatchers
 import kotlinx.coroutines.ExperimentalCoroutinesApi
 import kotlinx.coroutines.flow.MutableStateFlow
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
 import org.mockito.kotlin.whenever
 import javax.net.ssl.X509TrustManager
 
 @OptIn(ExperimentalCoroutinesApi::class)
 class ChatViewModelTest {
 
     private val testDispatcher = StandardTestDispatcher()
     private lateinit var context: Context
     private lateinit var translateAllMessagesUsersRepository: TranslateAllMessagesUsersRepository
     private lateinit var graphQLRepository: GraphQLRepository
     private lateinit var helixRepository: HelixRepository
     private lateinit var playerRepository: PlayerRepository
     private lateinit var emoteCache: EmoteCache
     private lateinit var okHttpClient: OkHttpClient
     private lateinit var json: Json
 
     @Before
     fun setup() {
         Dispatchers.setMain(testDispatcher)
         context = mock()
         translateAllMessagesUsersRepository = mock()
         graphQLRepository = mock()
         helixRepository = mock()
         playerRepository = mock()
         emoteCache = EmoteCache()
         okHttpClient = mock()
         json = Json { ignoreUnknownKeys = true }
     }
 
     @After
     fun tearDown() {
         Dispatchers.resetMain()
     }
 
     @Test
     fun `emoteCache initial state has null values`() {
         assertNull(emoteCache.globalBadges.value)
         assertNull(emoteCache.globalStvEmotes.value)
         assertNull(emoteCache.globalBttvEmotes.value)
         assertNull(emoteCache.globalFfzEmotes.value)
         assertNull(emoteCache.userEmotes.value)
     }
 
     @Test
     fun `emoteCache setGlobalBadges updates value`() = runTest {
         val badges = listOf(
             TwitchBadge(setId = "admin", version = "1", url1x = "https://example.com/admin.png"),
             TwitchBadge(setId = "moderator", version = "1", url1x = "https://example.com/mod.png")
         )
         
         emoteCache.setGlobalBadges(badges)
         
         assertEquals(badges, emoteCache.globalBadges.value)
         assertTrue(emoteCache.isGlobalBadgesCacheValid())
     }
 
     @Test
     fun `emoteCache setGlobalStvEmotes updates value`() = runTest {
         val emotes = listOf(
             Emote(name = "Kappa", url1x = "https://example.com/kappa.webp"),
             Emote(name = "PogChamp", url1x = "https://example.com/pog.webp")
         )
         
         emoteCache.setGlobalStvEmotes(emotes)
         
         assertEquals(emotes, emoteCache.globalStvEmotes.value)
         assertTrue(emoteCache.isGlobalStvEmotesCacheValid())
     }
 
     @Test
     fun `emoteCache setGlobalBttvEmotes updates value`() = runTest {
         val emotes = listOf(
             Emote(name = "catJAM", url1x = "https://example.com/catjam.gif")
         )
         
         emoteCache.setGlobalBttvEmotes(emotes)
         
         assertEquals(emotes, emoteCache.globalBttvEmotes.value)
         assertTrue(emoteCache.isGlobalBttvEmotesCacheValid())
     }
 
     @Test
     fun `emoteCache setGlobalFfzEmotes updates value`() = runTest {
         val emotes = listOf(
             Emote(name = "LULW", url1x = "https://example.com/lulw.png")
         )
         
         emoteCache.setGlobalFfzEmotes(emotes)
         
         assertEquals(emotes, emoteCache.globalFfzEmotes.value)
         assertTrue(emoteCache.isGlobalFfzEmotesCacheValid())
     }
 
     @Test
     fun `emoteCache setUserEmotes updates value`() = runTest {
         val emotes = listOf(
             TwitchEmote(id = "123", name = "TestEmote")
         )
         
         emoteCache.setUserEmotes(emotes)
         
         assertEquals(emotes, emoteCache.userEmotes.value)
         assertTrue(emoteCache.isUserEmotesCacheValid())
     }
 
     @Test
     fun `emoteCache clearGlobalCache clears all global caches`() = runTest {
         emoteCache.setGlobalBadges(listOf(TwitchBadge(setId = "test", version = "1", url1x = "url")))
         emoteCache.setGlobalStvEmotes(listOf(Emote(name = "test", url1x = "url")))
         emoteCache.setGlobalBttvEmotes(listOf(Emote(name = "test", url1x = "url")))
         emoteCache.setGlobalFfzEmotes(listOf(Emote(name = "test", url1x = "url")))
         
         emoteCache.clearGlobalCache()
         
         assertNull(emoteCache.globalBadges.value)
         assertNull(emoteCache.globalStvEmotes.value)
         assertNull(emoteCache.globalBttvEmotes.value)
         assertNull(emoteCache.globalFfzEmotes.value)
         assertFalse(emoteCache.isGlobalBadgesCacheValid())
         assertFalse(emoteCache.isGlobalStvEmotesCacheValid())
         assertFalse(emoteCache.isGlobalBttvEmotesCacheValid())
         assertFalse(emoteCache.isGlobalFfzEmotesCacheValid())
     }
 
     @Test
     fun `emoteCache clearUserCache clears user-specific caches`() = runTest {
         emoteCache.setUserEmotes(listOf(TwitchEmote(id = "123", name = "TestEmote")))
         emoteCache.setEmoteSets(listOf("0", "19151"))
         
         emoteCache.clearUserCache()
         
         assertNull(emoteCache.userEmotes.value)
         assertNull(emoteCache.emoteSets.value)
         assertFalse(emoteCache.isUserEmotesCacheValid())
     }
 
     @Test
     fun `emoteCache clearAll clears everything`() = runTest {
         emoteCache.setGlobalBadges(listOf(TwitchBadge(setId = "test", version = "1", url1x = "url")))
         emoteCache.setGlobalStvEmotes(listOf(Emote(name = "test", url1x = "url")))
         emoteCache.setUserEmotes(listOf(TwitchEmote(id = "123", name = "TestEmote")))
         emoteCache.setEmoteSets(listOf("0"))
         
         emoteCache.clearAll()
         
         assertNull(emoteCache.globalBadges.value)
         assertNull(emoteCache.globalStvEmotes.value)
         assertNull(emoteCache.globalBttvEmotes.value)
         assertNull(emoteCache.globalFfzEmotes.value)
         assertNull(emoteCache.userEmotes.value)
         assertNull(emoteCache.emoteSets.value)
     }
 
     @Test
     fun `ChatMessage creation with basic fields`() {
         val message = ChatMessage(
             id = "123",
             userId = "789",
             userLogin = "testuser",
             userName = "TestUser",
             message = "Hello world!",
             timestamp = 1234567890L
         )
         
         assertEquals("123", message.id)
         assertEquals("789", message.userId)
         assertEquals("testuser", message.userLogin)
         assertEquals("TestUser", message.userName)
         assertEquals("Hello world!", message.message)
         assertEquals(1234567890L, message.timestamp)
     }

    @Test
    fun `ChatMessage with emotes`() {
         val emotes = listOf(
             TwitchEmote(id = "25", name = "Kappa", begin = 0, end = 4)
         )
         val message = ChatMessage(
             id = "123",
             message = "Kappa test",
             emotes = emotes
         )
         
         assertEquals(1, message.emotes?.size)
         assertEquals("Kappa", message.emotes?.first()?.name)
         assertEquals(0, message.emotes?.first()?.begin)
         assertEquals(4, message.emotes?.first()?.end)
     }
 
     @Test
     fun `ChatMessage with system message`() {
         val message = ChatMessage(
             systemMsg = "User has been banned"
         )
         
         assertEquals("User has been banned", message.systemMsg)
         assertNull(message.message)
     }
 
     @Test
     fun `ChatMessage with reply`() {
         val parentMessage = ChatMessage(
             id = "parent123",
             userId = "user1",
             userName = "ParentUser",
             message = "Original message"
         )
         val replyMessage = ChatMessage(
             id = "reply456",
             userId = "user2",
             userName = "ReplyUser",
             message = "Reply message",
             isReply = true,
             replyParent = parentMessage
         )
         
         assertTrue(replyMessage.isReply)
         assertEquals("parent123", replyMessage.replyParent?.id)
         assertEquals("Original message", replyMessage.replyParent?.message)
     }
 
     @Test
     fun `TwitchBadge equality`() {
         val badge1 = TwitchBadge(setId = "subscriber", version = "12", url1x = "url1")
         val badge2 = TwitchBadge(setId = "subscriber", version = "12", url1x = "url1")
         val badge3 = TwitchBadge(setId = "subscriber", version = "24", url1x = "url2")
         
        assertEquals(badge1.setId, badge2.setId)
        assertEquals(badge1.version, badge2.version)
        assertEquals(badge1.url1x, badge2.url1x)
        assertNotEquals(badge1.version, badge3.version)
     }
 
     @Test
     fun `Emote with overlay flag`() {
         val normalEmote = Emote(name = "PogChamp", url1x = "url", isOverlayEmote = false)
         val overlayEmote = Emote(name = "cvHazmat", url1x = "url", isOverlayEmote = true)
         
         assertFalse(normalEmote.isOverlayEmote)
         assertTrue(overlayEmote.isOverlayEmote)
     }
 }
