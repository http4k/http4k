package sl4k

import org.http4k.connect.model.Base64Blob
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import sl4k.domain.ChannelName
import sl4k.domain.Emoji
import sl4k.domain.MessageText
import java.time.Duration

abstract class ChatContract {
    abstract val driver: ChatDriver

    private val general = ChannelName.of("general")
    private val random = ChannelName.of("random")
    private val secret = ChannelName.of("secret")
    private val solo = ChannelName.of("solo")
    private val design = ChannelName.of("design")
    private val nowhere = ChannelName.of("nowhere")

    private val morningAll = MessageText.of("Morning all")
    private val lunch = MessageText.of("Lunch?")
    private val tacos = MessageText.of("Tacos")
    private val plans = MessageText.of("Plans")
    private val shippedIt = MessageText.of("Shipped it")
    private val mine = MessageText.of("Mine")
    private val notes = FileToSend("notes.txt", "text/plain", Base64Blob.encode("Remember the milk"))
    private val photo = FileToSend("photo.png", "image/png", Base64Blob.encode(byteArrayOf(-119, 80, 78, 71)))

    @Test
    fun `a message posted in a channel shows for everyone in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).posts("Morning all"))

        eventually { assertEquals(ChannelSeen(listOf(Said(ann.name, morningAll))), bob.channels.open(general).sees()) }
    }

    @Test
    fun `a message stays in its own channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)

        ann.channels.open(general).posts("Morning all")

        assertEquals(ChannelSeen(emptyList()), bob.channels.open(random).sees())
    }

    @Test
    fun `a blank message is refused`() {
        assertEquals(Verdict.Rejected("Say something first"), driver.teammate("ann").channels.open(general).posts("   "))
    }

    @Test
    fun `posting to a channel that doesn't exist is refused`() {
        assertEquals(Verdict.Hidden, driver.teammate("ann").channels.open(nowhere).posts("Hello?"))
    }

    @Test
    fun `a channel shows its latest fifty messages, and older ones on request`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        (1..55).forEach { ann.channels.open(general).posts("message $it") }

        assertEquals(ChannelSeen((6..55).map { Said(ann.name, MessageText.of("message $it")) }, older = true), bob.channels.open(general).sees())

        bob.channels.open(general).showsOlder()

        assertEquals(ChannelSeen((1..55).map { Said(ann.name, MessageText.of("message $it")) }, older = false), bob.channels.open(general).sees())
    }

    @Test
    fun `a channel one teammate creates shows in everyone's sidebar`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.creates("design"))

        eventually { assertEquals(listOf(design, general, random), bob.channels.list()) }
    }

    @Test
    fun `a channel name that is taken or malformed is refused`() {
        val ann = driver.teammate("ann")

        assertEquals(Verdict.Rejected("#general already exists"), ann.channels.creates("general"))
        assertEquals(Verdict.Rejected("Use lowercase letters, numbers and dashes"), ann.channels.creates("Design Team"))
        assertEquals(Verdict.Rejected("Names starting dm- are for direct messages"), ann.channels.creates("dm-ann-bob"))
    }

    @Test
    fun `messages posted while you are in another channel show as unread`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(random)

        bob.channels.open(general).posts("Standup in five")
        bob.channels.open(general).posts("Actually, ten")

        eventually { assertEquals(mapOf(general to 2), ann.channels.unread()) }
    }

    @Test
    fun `opening a channel reads it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(random)
        bob.channels.open(general).posts("Standup in five")
        eventually { assertEquals(mapOf(general to 1), ann.channels.unread()) }

        ann.channels.open(general)

        eventually { assertEquals(emptyMap<ChannelName, Int>(), ann.channels.unread()) }
    }

    @Test
    fun `messages that arrive while you are in a channel are read when you leave it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general).posts("Standup in five")

        ann.channels.open(random)

        eventually { assertEquals(emptyMap<ChannelName, Int>(), ann.channels.unread()) }
    }

    @Test
    fun `messages that arrive after you leave a channel are unread`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        ann.channels.open(random)
        bob.channels.open(general)

        eventually(untilAClosedTabIsNoticed) {
            bob.channels.open(general).posts("Standup in five")
            assertEquals(setOf(general), ann.channels.unread().keys)
        }
    }

    @Test
    fun `your own messages are never unread for you`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).posts("Notes from yesterday")

        ann.channels.open(random)

        eventually { assertEquals(emptyMap<ChannelName, Int>(), ann.channels.unread()) }
    }

    @Test
    fun `everyone sees who is online, whichever channel they are in`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(random)

        eventually { assertEquals(setOf(ann.name, bob.name), ann.online()) }

        bob.closes()

        eventually(untilAClosedTabIsNoticed) { assertEquals(setOf(ann.name), ann.online()) }
    }

    @Test
    fun `being connected is being online, on any page`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.searches("anything")

        eventually { assertEquals(setOf(ann.name, bob.name), ann.online()) }
    }

    @Test
    fun `leaving a channel for another page leaves the channel but stays online`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val random = ChannelName.of("random")
        ann.channels.open(random)
        bob.channels.open(random)
        eventually { assertEquals(setOf(ann.name, bob.name), ann.online()) }

        bob.channels.searches("anything")

        eventually(untilAClosedTabIsNoticed) {
            ann.channels.open(random).posts("Still there?")
            assertEquals(setOf(random), bob.channels.unread().keys)
        }
        assertEquals(setOf(ann.name, bob.name), ann.online())
    }

    @Test
    fun `teammates see who is typing, until they stop for five seconds`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general)

        bob.channels.open(general).types("Hello th")

        eventually { assertEquals(setOf(bob.name), ann.channels.open(general).typing()) }
        assertEquals(emptySet<String>(), bob.channels.open(general).typing())

        driver.tick(Duration.ofSeconds(6))

        eventually { assertEquals(emptySet<String>(), ann.channels.open(general).typing()) }
    }

    @Test
    fun `a teammate who has never opened a channel sees the open channels, with nothing unread`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).posts("Morning")
        val bob = driver.teammate("bob")

        assertEquals(listOf(general, random), bob.channels.list())
        assertEquals(emptyMap<ChannelName, Int>(), bob.channels.unread())
    }

    @Test
    fun `a private channel is only listed for its members, and others can't read or post in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.createsPrivate(secret))

        assertEquals(listOf(general, random, secret), ann.channels.list())
        assertEquals(listOf(general, random), bob.channels.list())
        assertEquals(false, bob.channels.open(secret).canRead())
        assertEquals(Verdict.Hidden, bob.channels.open(secret).posts("Let me in"))
    }

    @Test
    fun `a member invites a teammate into a private channel, who can then read and post in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans for the offsite")

        assertEquals(Verdict.Accepted, ann.channels.open(secret).invites(bob))

        eventually { assertEquals(listOf(general, random, secret), bob.channels.list()) }
        assertEquals(ChannelSeen(listOf(Said(ann.name, MessageText.of("Plans for the offsite")))), bob.channels.open(secret).sees())
        assertEquals(Verdict.Accepted, bob.channels.open(secret).posts("Count me in"))
    }

    @Test
    fun `only members of a private channel can invite people into it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)

        assertEquals(Verdict.Hidden, bob.channels.open(secret).invites(cat))
    }

    @Test
    fun `a public channel has nobody to invite, and can't be left`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Denied, ann.channels.open(general).invites(bob))
        assertEquals(Verdict.Denied, ann.channels.open(general).leaves())
    }

    @Test
    fun `a direct message is a private channel of two, listed under the other person's name`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)

        val dm = ann.channels.messagesDirectly(bob)
        ann.channels.open(dm).posts("Got a minute?")

        eventually { assertEquals(mapOf(ann.name to 1), bob.channels.directMessages()) }
        assertEquals(emptyMap<String, Int>(), cat.channels.directMessages())
        assertEquals(Verdict.Hidden, cat.channels.open(dm).posts("Hi both"))
    }

    @Test
    fun `a channel is headed with its name, and a direct message with the other person's`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        val dm = ann.channels.messagesDirectly(bob)

        assertEquals("#general", ann.channels.open(general).heading())
        assertEquals(bob.name, ann.channels.open(dm).heading())
        assertEquals(ann.name, bob.channels.open(dm).heading())
    }

    @Test
    fun `a mention names the teammate in the message`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("@${bob.handle} can you look at this?")

        eventually { assertEquals(ChannelSeen(listOf(Said(ann.name, MessageText.of("@${bob.handle} can you look at this?"), setOf(bob.name)))), bob.channels.open(general).sees()) }
    }

    @Test
    fun `a mention still names the teammate inside bold text`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("*ask @${bob.handle} first*")

        eventually { assertEquals(setOf(bob.name), bob.channels.open(general).sees().messages.single().mentions) }
    }

    @Test
    fun `search finds messages across channels, newest first, whatever the case`() {
        val ann = driver.teammate("ann")
        val random = ChannelName.of("random")
        ann.channels.open(general).posts("Lunch at noon?")
        driver.tick(Duration.ofSeconds(1))
        ann.channels.open(random).posts("who wants lunch")
        ann.channels.open(random).posts("something else")

        assertEquals(listOf(Found("#random", ann.name, MessageText.of("who wants lunch")), Found("#general", ann.name, MessageText.of("Lunch at noon?"))), ann.channels.searches("LUNCH"))
    }

    @Test
    fun `search finds replies in threads`() {
        val ann = driver.teammate("ann")
        val lunch = MessageText.of("Lunch?")
        ann.channels.open(general).posts(lunch.value)
        ann.channels.open(general).message(lunch).replies("Tacos please")

        assertEquals(listOf(MessageText.of("Tacos please")), ann.channels.searches("tacos").map { it.text })
    }

    @Test
    fun `search only looks in channels you can see`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("the launch date")
        val dm = ann.channels.messagesDirectly(bob)
        ann.channels.open(dm).posts("launch is close")

        assertEquals(listOf(MessageText.of("launch is close"), MessageText.of("the launch date")), ann.channels.searches("launch").map { it.text }.sortedBy { it.value })
        assertEquals(listOf(Found(ann.name, ann.name, MessageText.of("launch is close"))), bob.channels.searches("launch"))
    }

    @Test
    fun `search results stay live`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        assertEquals(emptyList<Found>(), bob.channels.searches("lunch"))

        ann.channels.open(general).posts("Lunch?")
        eventually { assertEquals(listOf(MessageText.of("Lunch?")), bob.channels.searches("lunch").map { it.text }) }

        ann.channels.open(general).message(MessageText.of("Lunch?")).edits("Dinner?")
        eventually { assertEquals(emptyList<Found>(), bob.channels.searches("lunch")) }
    }

    @Test
    fun `being removed from a channel takes its messages out of your results while you watch`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).posts("the launch date")
        assertEquals(1, bob.channels.searches("launch").size)

        ann.channels.open(secret).removes(bob)

        eventually { assertEquals(emptyList<Found>(), bob.channels.searches("launch")) }
    }

    @Test
    fun `search doesn't find deleted messages, and a blank search finds nothing`() {
        val ann = driver.teammate("ann")
        val lunch = MessageText.of("Lunch?")
        ann.channels.open(general).posts(lunch.value)
        ann.channels.open(general).message(lunch).deletes()

        assertEquals(emptyList<Found>(), ann.channels.searches("lunch"))
        ann.channels.open(general).posts("Lunch again?")
        assertEquals(emptyList<Found>(), ann.channels.searches(""))
    }

    @Test
    fun `a link's preview appears for everyone watching, after the message`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("read https://example.test/article please")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertEquals(LinkPreview("An article", "All about it"), bob.channels.open(general).sees().messages.single().preview) }
    }

    @Test
    fun `each link in a message gets its own preview, in order`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/plain and https://example.test/article")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertEquals(listOf(LinkPreview("A plain page", "Nothing fancy"), LinkPreview("An article", "All about it")), ann.channels.open(general).sees().messages.single().previews) }
    }

    @Test
    fun `a preview shows the page's image from our own server`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/pictured")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertEquals(LinkPreview("Pictured", "", true), ann.channels.open(general).sees().messages.single().preview) }
    }

    @Test
    fun `a page with no open graph tags previews its title and description`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/plain")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertEquals(LinkPreview("A plain page", "Nothing fancy"), ann.channels.open(general).sees().messages.single().preview) }
    }

    @Test
    fun `a preview follows a redirect`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/moved")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertEquals(LinkPreview("An article", "All about it"), ann.channels.open(general).sees().messages.single().preview) }
    }

    @Test
    fun `editing a link away removes its preview, and editing one in adds it`() {
        val ann = driver.teammate("ann").channels.open(general)
        ann.posts("https://example.test/article")
        driver.tick(Duration.ofSeconds(2))
        eventually { assertEquals("An article", ann.sees().messages.single().preview?.title) }

        ann.message(MessageText.of("https://example.test/article")).edits("https://example.test/plain")
        driver.tick(Duration.ofSeconds(2))
        eventually { assertEquals("A plain page", ann.sees().messages.single().preview?.title) }

        ann.message(MessageText.of("https://example.test/plain")).edits("no link now")
        eventually { assertEquals(null, ann.sees().messages.single().preview) }
    }

    @Test
    fun `deleting a message removes its preview`() {
        val ann = driver.teammate("ann").channels.open(general)
        val link = MessageText.of("https://example.test/article")
        ann.posts(link.value)
        driver.tick(Duration.ofSeconds(2))
        eventually { assertEquals("An article", ann.sees().messages.single().preview?.title) }

        ann.message(link).replies("kept alive by this reply")
        ann.message(link).deletes()

        eventually { assertEquals(null, ann.sees().messages.single().preview) }
    }

    @Test
    fun `an @ that matches nobody who can see the channel is just text`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)

        ann.channels.open(general).posts("@nobody-here hello")
        ann.channels.open(secret).posts("@${bob.handle} you can't see this")

        assertEquals(ChannelSeen(listOf(Said(ann.name, MessageText.of("@nobody-here hello")))), ann.channels.open(general).sees())
        assertEquals(ChannelSeen(listOf(Said(ann.name, MessageText.of("@${bob.handle} you can't see this")))), ann.channels.open(secret).sees())
    }

    @Test
    fun `unread mentions are counted in the sidebar until the channel is read`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)

        ann.channels.open(general).posts("Morning all")
        ann.channels.open(general).posts("@${bob.handle} can you look at this?")

        eventually { assertEquals(mapOf(general to 1), bob.channels.mentions()) }
        assertEquals(mapOf(general to 2), bob.channels.unread())

        bob.channels.open(general)

        eventually { assertEquals(emptyMap<ChannelName, Int>(), bob.channels.mentions()) }
    }

    @Test
    fun `typing @ suggests the other teammates who can see the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertEquals(setOf(bob.name), ann.channels.open(secret).suggestions("thoughts @"))
        assertEquals(setOf(bob.name, cat.name), ann.channels.open(general).suggestions("thoughts @"))
        assertEquals(setOf(cat.name), ann.channels.open(general).suggestions("thoughts @${cat.handle.take(3)}"))
    }

    @Test
    fun `tab completes the handle being typed`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertEquals("thoughts @${bob.handle} ", ann.channels.open(secret).tabs("thoughts @${bob.handle.take(2)}"))
    }

    @Test
    fun `replies show in the thread for everyone looking at it, in order, and not in the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general).message(lunch).opensThread()

        assertEquals(Verdict.Accepted, ann.channels.open(general).message(lunch).replies("Tacos"))
        assertEquals(Verdict.Accepted, bob.channels.open(general).message(lunch).replies("Seconded"))

        eventually {
            assertEquals(ThreadSeen(Said(ann.name, lunch, replies = 2), listOf(Said(ann.name, tacos), Said(bob.name, MessageText.of("Seconded")))), bob.channels.open(general).message(lunch).thread())
        }
        assertEquals(listOf(Said(ann.name, lunch, replies = 2)), ann.channels.open(general).sees().messages)
    }

    @Test
    fun `everyone in the channel sees how many replies a message has`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general)

        ann.channels.open(general).message(lunch).replies("Tacos")

        eventually { assertEquals(listOf(Said(ann.name, lunch, replies = 1)), bob.channels.open(general).sees().messages) }
    }

    @Test
    fun `replying to a reply, with nothing to say, or where you can't see the channel is refused`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.open(general).posts("Lunch?")
        ann.channels.open(general).message(lunch).replies("Tacos")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertEquals(Verdict.Rejected("Threads are one level deep"), bob.channels.open(general).message(tacos).replies("Burritos"))
        assertEquals(Verdict.Rejected("Say something first"), bob.channels.open(general).message(lunch).replies("   "))
        assertEquals(Verdict.Hidden, bob.channels.open(secret).message(plans).replies("Me too"))
    }

    @Test
    fun `a reaction shows its count for everyone in the channel, marked as yours only for you`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).message(shippedIt).reacts(Emoji.tada))

        eventually { assertEquals(listOf(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1))), bob.channels.open(general).sees().messages) }
        assertEquals(listOf(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1), mine = setOf(Emoji.tada))), ann.channels.open(general).sees().messages)
    }

    @Test
    fun `the same reaction from two teammates counts twice, and reacting again takes yours back`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).reacts(Emoji.tada)
        bob.channels.open(general).message(shippedIt).reacts(Emoji.tada)
        eventually { assertEquals(mapOf(Emoji.tada to 2), ann.channels.open(general).sees().messages.single().reactions) }

        bob.channels.open(general).message(shippedIt).reacts(Emoji.tada)

        eventually { assertEquals(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1), mine = setOf(Emoji.tada)), ann.channels.open(general).sees().messages.single()) }
        assertEquals(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1)), bob.channels.open(general).sees().messages.single())
    }

    @Test
    fun `replies in a thread take reactions too`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        ann.channels.open(general).message(lunch).replies("Tacos")
        bob.channels.open(general).message(lunch).opensThread()

        ann.channels.open(general).message(tacos).reacts(Emoji.heart)

        eventually { assertEquals(listOf(Said(ann.name, tacos, reactions = mapOf(Emoji.heart to 1))), bob.channels.open(general).message(lunch).thread().replies) }
    }

    @Test
    fun `reacting where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertEquals(Verdict.Hidden, bob.channels.open(secret).message(plans).reacts(Emoji.eyes))
    }

    @Test
    fun `an author's edit shows for everyone, marked as edited`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch at 12")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).message(MessageText.of("Lunch at 12")).edits(to = "Lunch at 1"))

        eventually { assertEquals(listOf(Said(ann.name, MessageText.of("Lunch at 1"), edited = true)), bob.channels.open(general).sees().messages) }
    }

    @Test
    fun `a deleted message leaves everyone's history`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Wrong channel, sorry")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).message(MessageText.of("Wrong channel, sorry")).deletes())

        eventually { assertEquals(ChannelSeen(emptyList()), bob.channels.open(general).sees()) }
    }

    @Test
    fun `a divider sits above the first message you haven't read`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Morning")
        bob.channels.open(general)
        bob.channels.open(random)

        eventually(untilAClosedTabIsNoticed) {
            ann.channels.open(general).posts("Standup")
            assertEquals(setOf(general), bob.channels.unread().keys)
        }
        ann.channels.open(general).posts("Lunch")

        assertEquals("Standup", bob.channels.open(general).firstUnread())
    }

    @Test
    fun `the divider is gone once you have read the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Morning")
        eventually { assertEquals(mapOf(general to 1), bob.channels.unread()) }

        bob.channels.open(general)

        eventually(untilAClosedTabIsNoticed) {
            bob.channels.open(random)
            assertEquals(null, bob.channels.open(general).firstUnread())
        }
    }

    @Test
    fun `the divider skips a message deleted before you read it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Wrong channel")
        ann.channels.open(general).posts("Morning")
        ann.channels.open(general).message(MessageText.of("Wrong channel")).deletes()
        eventually { assertEquals(mapOf(general to 1), bob.channels.unread()) }

        assertEquals("Morning", bob.channels.open(general).firstUnread())
    }

    @Test
    fun `deleting a message someone hasn't read takes it off their unread count and their mentions`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("@${bob.handle} wrong person")
        ann.channels.open(general).posts("Morning all")
        eventually { assertEquals(mapOf(general to 2), bob.channels.unread()) }
        assertEquals(mapOf(general to 1), bob.channels.mentions())

        ann.channels.open(general).message(MessageText.of("@${bob.handle} wrong person")).deletes()

        eventually { assertEquals(mapOf(general to 1), bob.channels.unread()) }
        assertEquals(emptyMap<ChannelName, Int>(), bob.channels.mentions())
    }

    @Test
    fun `editing a mention in counts it for the teammate mentioned`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Have a look")

        ann.channels.open(general).message(MessageText.of("Have a look")).edits(to = "@${bob.handle} have a look")

        eventually { assertEquals(mapOf(general to 1), bob.channels.mentions()) }
    }

    @Test
    fun `only the author can edit or delete a message, and an edit can't blank it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.open(general).posts("Mine")

        assertEquals(Verdict.Denied, bob.channels.open(general).message(mine).edits(to = "Yours"))
        assertEquals(Verdict.Denied, bob.channels.open(general).message(mine).deletes())
        assertEquals(Verdict.Rejected("Say something first"), ann.channels.open(general).message(mine).edits(to = "   "))
    }

    @Test
    fun `a deleted message with replies stays, so its thread still makes sense`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general).message(lunch).replies("Tacos")

        ann.channels.open(general).message(lunch).deletes()

        val tombstone = Said(ann.name, MessageText.of("This message was deleted"), replies = 1, deleted = true)
        eventually { assertEquals(listOf(tombstone), bob.channels.open(general).sees().messages) }
        assertEquals(ThreadSeen(tombstone, listOf(Said(bob.name, tacos))), bob.channels.open(general).message(lunch).thread())
    }

    @Test
    fun `a member who leaves a private channel loses it, and can't read it any more`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertEquals(Verdict.Accepted, bob.channels.open(secret).leaves())

        eventually { assertEquals(listOf(general, random), bob.channels.list()) }
        assertEquals(false, bob.channels.open(secret).canRead())
    }

    @Test
    fun `the owner removes a teammate who is watching, and they land in general without the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        bob.channels.open(secret)

        assertEquals(Verdict.Accepted, ann.channels.open(secret).removes(bob))

        eventually { assertEquals(general, bob.viewing()) }
        eventually { assertEquals(listOf(general, random), bob.channels.list()) }
    }

    @Test
    fun `only the owner removes people, and a direct message can't be left`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).invites(cat)
        val dm = ann.channels.messagesDirectly(bob)

        assertEquals(Verdict.Denied, bob.channels.open(secret).removes(cat))
        assertEquals(Verdict.Denied, ann.channels.open(dm).leaves())
    }

    @Test
    fun `the last member leaving deletes the channel and everything in it`() {
        val ann = driver.teammate("ann")
        ann.channels.createsPrivate(solo)
        ann.channels.open(solo).posts("Mine")
        ann.channels.open(solo).topic.sets("Just me")
        ann.channels.open(solo).bookmarks.adds("Runbook", "https://example.com/runbook")
        ann.channels.open(solo).canvas.saves("Notes")

        assertEquals(Verdict.Accepted, ann.channels.open(solo).leaves())

        eventually { assertEquals(listOf(general, random), ann.channels.list()) }

        ann.channels.createsPrivate(solo)

        assertEquals(ChannelSeen(emptyList()), ann.channels.open(solo).sees())
        assertEquals("", ann.channels.open(solo).topic.read())
        assertEquals(emptyList<Bookmarked>(), ann.channels.open(solo).bookmarks.list())
        assertEquals("", ann.channels.open(solo).canvas.read())
    }

    @Test
    fun `a file sent to a channel shows for everyone and downloads as it was sent`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).attaches(notes, "Shipped it"))

        eventually { assertEquals(listOf(Attached("notes.txt", inline = false)), bob.channels.open(general).message(shippedIt).attachments()) }
        assertEquals(Downloaded(notes.contents, "application/octet-stream", inline = false), bob.channels.open(general).downloads("notes.txt"))
    }

    @Test
    fun `an image shows inline, and a file with nothing to say goes under its own name`() {
        val ann = driver.teammate("ann")

        assertEquals(Verdict.Accepted, ann.channels.open(general).attaches(photo))

        assertEquals(listOf(Attached("photo.png", inline = true)), ann.channels.open(general).message(MessageText.of("photo.png")).attachments())
        assertEquals(Downloaded(photo.contents, "image/png", inline = true), ann.channels.open(general).downloads("photo.png"))
    }

    @Test
    fun `a file that could run in the page never shows inline, whatever it claims to be`() {
        val ann = driver.teammate("ann")
        val script = Base64Blob.encode("<script>alert(1)</script>")

        ann.channels.open(general).attaches(FileToSend("page.html", "text/html", script), "Plans")
        ann.channels.open(general).attaches(FileToSend("logo.svg", "image/svg+xml", script), "Mine")

        assertEquals(listOf(Attached("page.html", inline = false)), ann.channels.open(general).message(plans).attachments())
        assertEquals(listOf(Attached("logo.svg", inline = false)), ann.channels.open(general).message(mine).attachments())
        assertEquals(Downloaded(script, "application/octet-stream", inline = false), ann.channels.open(general).downloads("logo.svg"))
    }

    @Test
    fun `several files go with one message, by name whatever order they arrive in`() {
        val ann = driver.teammate("ann")
        val files = (1..5).map { FileToSend("file$it.txt", "text/plain", Base64Blob.encode("Number $it")) }

        assertEquals(Verdict.Accepted, ann.channels.open(general).attachesAll(files.reversed(), "All of them"))

        assertEquals(files.map { Attached(it.name, inline = false) }, ann.channels.open(general).message(MessageText.of("All of them")).attachments())
    }

    @Test
    fun `a message with several files and nothing to say goes under the first by name`() {
        val ann = driver.teammate("ann")

        assertEquals(Verdict.Accepted, ann.channels.open(general).attachesAll(listOf(photo, notes)))

        assertEquals(listOf("notes.txt"), ann.channels.open(general).sees().messages.map { it.text.value })
    }

    @Test
    fun `more than ten files at once are refused, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val files = (1..11).map { FileToSend("file$it.txt", "text/plain", Base64Blob.encode("Number $it")) }

        assertEquals(Verdict.Rejected("Send up to 10 files at once"), ann.channels.open(general).attachesAll(files, "Too many"))

        assertEquals(ChannelSeen(emptyList()), ann.channels.open(general).sees())
    }

    @Test
    fun `one bad file refuses them all, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val huge = FileToSend("huge.bin", "application/octet-stream", Base64Blob.encode(ByteArray(5 * 1024 * 1024 + 1)))

        assertEquals(Verdict.Rejected("Keep files to 5 MB"), ann.channels.open(general).attachesAll(listOf(notes, huge), "Shipped it"))

        assertEquals(ChannelSeen(emptyList()), ann.channels.open(general).sees())
    }

    @Test
    fun `a file over five megabytes is refused, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val huge = FileToSend("huge.bin", "application/octet-stream", Base64Blob.encode(ByteArray(5 * 1024 * 1024 + 1)))

        assertEquals(Verdict.Rejected("Keep files to 5 MB"), ann.channels.open(general).attaches(huge, "Shipped it"))

        assertEquals(ChannelSeen(emptyList()), ann.channels.open(general).sees())
    }

    @Test
    fun `only people who can see the channel can send or fetch its files`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).attaches(notes, "Plans")

        assertEquals(notes.contents, bob.channels.open(secret).downloads("notes.txt")?.contents)
        assertEquals(Verdict.Hidden, cat.channels.open(secret).attaches(photo))
        assertEquals(null, cat.channels.open(secret).downloads("notes.txt"))

        ann.channels.open(secret).removes(bob)

        eventually { assertEquals(null, bob.channels.open(secret).downloads("notes.txt")) }
    }

    @Test
    fun `deleting a message takes its file with it`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).attaches(notes, "Plans")

        ann.channels.open(general).message(plans).deletes()

        assertEquals(null, ann.channels.open(general).downloads("notes.txt"))
    }

    @Test
    fun `a channel's topic shows for everyone in it, and only in that channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).topic.sets("Shipping on Friday"))

        eventually { assertEquals("Shipping on Friday", bob.channels.open(general).topic.read()) }
        assertEquals("", bob.channels.open(random).topic.read())
    }

    @Test
    fun `a blank topic clears it, and a long one is refused`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).topic.sets("Shipping on Friday")

        assertEquals(Verdict.Rejected("Keep the topic to 250 characters"), ann.channels.open(general).topic.sets("x".repeat(251)))
        assertEquals("Shipping on Friday", ann.channels.open(general).topic.read())

        assertEquals(Verdict.Accepted, ann.channels.open(general).topic.sets("   "))
        assertEquals("", ann.channels.open(general).topic.read())
    }

    @Test
    fun `setting the topic where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)

        assertEquals(Verdict.Hidden, bob.channels.open(secret).topic.sets("Peeking"))
    }

    @Test
    fun `bookmarks show for everyone in the channel, in the order they were added`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).bookmarks.adds("Runbook", "https://example.com/runbook"))
        assertEquals(Verdict.Accepted, ann.channels.open(general).bookmarks.adds("Dashboard", "http://example.com/dash?team=acme&view=1"))

        eventually {
            assertEquals(listOf(Bookmarked("Runbook", "https://example.com/runbook"), Bookmarked("Dashboard", "http://example.com/dash?team=acme&view=1")), bob.channels.open(general).bookmarks.list())
        }
        assertEquals(emptyList<Bookmarked>(), bob.channels.open(random).bookmarks.list())
    }

    @Test
    fun `anyone in the channel can remove a bookmark`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).bookmarks.adds("Runbook", "https://example.com/runbook")
        ann.channels.open(general)

        assertEquals(Verdict.Accepted, bob.channels.open(general).bookmarks.removes("Runbook"))

        eventually { assertEquals(emptyList<Bookmarked>(), ann.channels.open(general).bookmarks.list()) }
    }

    @Test
    fun `a bookmark needs a title and a web link`() {
        val ann = driver.teammate("ann")

        assertEquals(Verdict.Rejected("Give the bookmark a title"), ann.channels.open(general).bookmarks.adds("  ", "example.com"))
        assertEquals(Verdict.Rejected("Use an http or https link"), ann.channels.open(general).bookmarks.adds("Sneaky", "javascript:alert(1)"))
        assertEquals(Verdict.Rejected("Use an http or https link"), ann.channels.open(general).bookmarks.adds("Files", "ftp://example.com/x"))
        assertEquals(emptyList<Bookmarked>(), ann.channels.open(general).bookmarks.list())
    }

    @Test
    fun `changing bookmarks where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).bookmarks.adds("Plans", "https://example.com/plans")

        assertEquals(Verdict.Hidden, bob.channels.open(secret).bookmarks.adds("Peeking", "https://example.com/peek"))
        assertEquals(Verdict.Hidden, bob.channels.open(secret).bookmarks.removes("Plans"))
    }

    @Test
    fun `a pinned message shows at the top for everyone until someone unpins it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).posts("Morning all")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).message(shippedIt).togglesPin())

        eventually { assertEquals(listOf(Pinned(ann.name, "Shipped it")), bob.channels.open(general).pinned()) }
        assertEquals(emptyList<Pinned>(), bob.channels.open(random).pinned())

        assertEquals(Verdict.Accepted, bob.channels.open(general).message(shippedIt).togglesPin())

        eventually { assertEquals(emptyList<Pinned>(), ann.channels.open(general).pinned()) }
    }

    @Test
    fun `clicking the pin at the top of the channel unpins the message for everyone`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).togglesPin()
        ann.channels.open(general)

        assertEquals(Verdict.Accepted, bob.channels.open(general).message(shippedIt).unpinsFromTheTop())

        eventually { assertEquals(emptyList<Pinned>(), ann.channels.open(general).pinned()) }
    }

    @Test
    fun `a pin follows its message's edits, and goes when the message is deleted`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).togglesPin()
        bob.channels.open(general)

        ann.channels.open(general).message(shippedIt).edits("Shipped it twice")

        eventually { assertEquals(listOf(Pinned(ann.name, "Shipped it twice")), bob.channels.open(general).pinned()) }

        ann.channels.open(general).message(MessageText.of("Shipped it twice")).deletes()

        eventually { assertEquals(emptyList<Pinned>(), bob.channels.open(general).pinned()) }
    }

    @Test
    fun `pinning where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertEquals(Verdict.Hidden, bob.channels.open(secret).message(plans).togglesPin())
    }

    @Test
    fun `a saved canvas shows for everyone in the channel, and only in that channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertEquals(Verdict.Accepted, ann.channels.open(general).canvas.saves("Release checklist\n- tag\n- deploy"))

        eventually { assertEquals("Release checklist\n- tag\n- deploy", bob.channels.open(general).canvas.read()) }
        assertEquals("", bob.channels.open(random).canvas.read())
    }

    @Test
    fun `a teammate picks up where the last save left off`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).canvas.saves("First draft")

        assertEquals(Verdict.Accepted, bob.channels.open(general).canvas.saves("Second draft"))
        assertEquals(Verdict.Accepted, ann.channels.open(general).canvas.saves("Third draft"))

        assertEquals("Third draft", bob.channels.open(general).canvas.read())
    }

    @Test
    fun `saving over a teammate's newer save is refused, and their text survives`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).canvas.saves("First draft")
        ann.channels.open(general).canvas.begins()
        bob.channels.open(general).canvas.saves("Bob's draft")

        assertEquals(Verdict.Rejected("A teammate saved first. Copy your text, then edit again"), ann.channels.open(general).canvas.saves("Ann's draft"))
        assertEquals("Bob's draft", bob.channels.open(general).canvas.read())

        ann.channels.open(general).canvas.begins()

        assertEquals(Verdict.Accepted, ann.channels.open(general).canvas.saves("Ann's draft"))
        assertEquals("Ann's draft", bob.channels.open(general).canvas.read())
    }

    @Test
    fun `a blank canvas clears it, and an enormous one is refused`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).canvas.saves("First draft")

        assertEquals(Verdict.Rejected("Keep the canvas to 20000 characters"), ann.channels.open(general).canvas.saves("x".repeat(20001)))
        assertEquals("First draft", ann.channels.open(general).canvas.read())

        ann.channels.open(general).canvas.begins()

        assertEquals(Verdict.Accepted, ann.channels.open(general).canvas.saves(""))
        assertEquals("", ann.channels.open(general).canvas.read())
    }

    @Test
    fun `saving a canvas where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)

        assertEquals(Verdict.Hidden, bob.channels.open(secret).canvas.saves("Peeking"))
    }

    @Test
    fun `each teammate sees which person in the list is them`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general)

        eventually { assertEquals(ann.name, ann.you()) }
        eventually { assertEquals(bob.name, bob.you()) }
    }
}