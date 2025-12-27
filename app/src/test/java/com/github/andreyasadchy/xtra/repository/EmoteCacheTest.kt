 package com.github.andreyasadchy.xtra.repository
 
 import com.github.andreyasadchy.xtra.model.chat.Emote
 import com.github.andreyasadchy.xtra.model.chat.TwitchBadge
 import com.github.andreyasadchy.xtra.model.chat.TwitchEmote
 import kotlinx.coroutines.ExperimentalCoroutinesApi
 import kotlinx.coroutines.test.runTest
 import org.junit.Assert.*
 import org.junit.Before
 import org.junit.Test
 
 @OptIn(ExperimentalCoroutinesApi::class)
 class EmoteCacheTest {
 
     private lateinit var emoteCache: EmoteCache
 
     @Before
     fun setup() {
         emoteCache = EmoteCache()
     }
 
     @Test
     fun `initial state has null values`() {
         assertNull(emoteCache.globalBadges.value)
         assertNull(emoteCache.globalStvEmotes.value)
         assertNull(emoteCache.globalBttvEmotes.value)
         assertNull(emoteCache.globalFfzEmotes.value)
         assertNull(emoteCache.userEmotes.value)
         assertNull(emoteCache.emoteSets.value)
     }
 
     @Test
     fun `setGlobalBadges updates value`() = runTest {
         val badges = listOf(
             TwitchBadge(setId = "admin", version = "1", url1x = "https://example.com/admin.png"),
             TwitchBadge(setId = "moderator", version = "1", url1x = "https://example.com/mod.png")
         )
         
         emoteCache.setGlobalBadges(badges)
         
         assertEquals(badges, emoteCache.globalBadges.value)
         assertTrue(emoteCache.isGlobalBadgesCacheValid())
     }
 
     @Test
     fun `setGlobalStvEmotes updates value`() = runTest {
         val emotes = listOf(
             Emote(name = "Kappa", url1x = "https://example.com/kappa.webp"),
             Emote(name = "PogChamp", url1x = "https://example.com/pog.webp")
         )
         
         emoteCache.setGlobalStvEmotes(emotes)
         
         assertEquals(emotes, emoteCache.globalStvEmotes.value)
         assertTrue(emoteCache.isGlobalStvEmotesCacheValid())
     }
 
     @Test
     fun `setGlobalBttvEmotes updates value`() = runTest {
         val emotes = listOf(
             Emote(name = "catJAM", url1x = "https://example.com/catjam.gif")
         )
         
         emoteCache.setGlobalBttvEmotes(emotes)
         
         assertEquals(emotes, emoteCache.globalBttvEmotes.value)
         assertTrue(emoteCache.isGlobalBttvEmotesCacheValid())
     }
 
     @Test
     fun `setGlobalFfzEmotes updates value`() = runTest {
         val emotes = listOf(
             Emote(name = "LULW", url1x = "https://example.com/lulw.png")
         )
         
         emoteCache.setGlobalFfzEmotes(emotes)
         
         assertEquals(emotes, emoteCache.globalFfzEmotes.value)
         assertTrue(emoteCache.isGlobalFfzEmotesCacheValid())
     }
 
     @Test
     fun `setUserEmotes updates value`() = runTest {
         val emotes = listOf(
             TwitchEmote(id = "123", name = "TestEmote")
         )
         
         emoteCache.setUserEmotes(emotes)
         
         assertEquals(emotes, emoteCache.userEmotes.value)
         assertTrue(emoteCache.isUserEmotesCacheValid())
     }
 
     @Test
     fun `setEmoteSets updates value`() = runTest {
         val sets = listOf("0", "19151", "300374282")
         
         emoteCache.setEmoteSets(sets)
         
         assertEquals(sets, emoteCache.emoteSets.value)
     }
 
     @Test
     fun `clearGlobalCache clears all global caches`() = runTest {
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
     fun `clearUserCache clears user-specific caches`() = runTest {
         emoteCache.setUserEmotes(listOf(TwitchEmote(id = "123", name = "TestEmote")))
         emoteCache.setEmoteSets(listOf("0", "19151"))
         
         emoteCache.clearUserCache()
         
         assertNull(emoteCache.userEmotes.value)
         assertNull(emoteCache.emoteSets.value)
         assertFalse(emoteCache.isUserEmotesCacheValid())
     }
 
     @Test
     fun `clearAll clears everything`() = runTest {
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
     fun `cache validity is false for null values`() {
         assertFalse(emoteCache.isGlobalBadgesCacheValid())
         assertFalse(emoteCache.isGlobalStvEmotesCacheValid())
         assertFalse(emoteCache.isGlobalBttvEmotesCacheValid())
         assertFalse(emoteCache.isGlobalFfzEmotesCacheValid())
         assertFalse(emoteCache.isUserEmotesCacheValid())
     }
 
     @Test
     fun `setting null values makes cache invalid`() = runTest {
         emoteCache.setGlobalBadges(listOf(TwitchBadge(setId = "test", version = "1", url1x = "url")))
         assertTrue(emoteCache.isGlobalBadgesCacheValid())
         
         emoteCache.setGlobalBadges(null)
         assertFalse(emoteCache.isGlobalBadgesCacheValid())
     }
 }
