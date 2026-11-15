package sl4k

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.connect.model.Base64Blob
import org.http4k.fanbelt.testing.Verdict
import org.http4k.fanbelt.testing.eventually
import org.http4k.fanbelt.testing.untilAClosedTabIsNoticed
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

        assertThat(ann.channels.open(general).posts("Morning all"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, morningAll))))) }
    }

    @Test
    fun `a message stays in its own channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)

        ann.channels.open(general).posts("Morning all")

        assertThat(bob.channels.open(random).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `a blank message is refused`() {
        assertThat(driver.teammate("ann").channels.open(general).posts("   "), equalTo(Verdict.Rejected("Say something first")))
    }

    @Test
    fun `posting to a channel that doesn't exist is refused`() {
        assertThat(driver.teammate("ann").channels.open(nowhere).posts("Hello?"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a channel shows its latest fifty messages, and older ones on request`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        (1..55).forEach { ann.channels.open(general).posts("message $it") }

        assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen((6..55).map { Said(ann.name, MessageText.of("message $it")) }, older = true)))

        bob.channels.open(general).showsOlder()

        assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen((1..55).map { Said(ann.name, MessageText.of("message $it")) }, older = false)))
    }

    @Test
    fun `a channel one teammate creates shows in everyone's sidebar`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.creates("design"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.list(), equalTo(listOf(design, general, random))) }
    }

    @Test
    fun `a channel name that is taken or malformed is refused`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.creates("general"), equalTo(Verdict.Rejected("#general already exists")))
        assertThat(ann.channels.creates("Design Team"), equalTo(Verdict.Rejected("Use lowercase letters, numbers and dashes")))
        assertThat(ann.channels.creates("dm-ann-bob"), equalTo(Verdict.Rejected("Names starting dm- are for direct messages")))
    }

    @Test
    fun `messages posted while you are in another channel show as unread`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(random)

        bob.channels.open(general).posts("Standup in five")
        bob.channels.open(general).posts("Actually, ten")

        eventually { assertThat(ann.channels.unread(), equalTo(mapOf(general to 2))) }
    }

    @Test
    fun `opening a channel reads it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(random)
        bob.channels.open(general).posts("Standup in five")
        eventually { assertThat(ann.channels.unread(), equalTo(mapOf(general to 1))) }

        ann.channels.open(general)

        eventually { assertThat(ann.channels.unread(), equalTo(emptyMap<ChannelName, Int>())) }
    }

    @Test
    fun `messages that arrive while you are in a channel are read when you leave it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general).posts("Standup in five")

        ann.channels.open(random)

        eventually { assertThat(ann.channels.unread(), equalTo(emptyMap<ChannelName, Int>())) }
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
            assertThat(ann.channels.unread().keys, equalTo(setOf(general)))
        }
    }

    @Test
    fun `your own messages are never unread for you`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).posts("Notes from yesterday")

        ann.channels.open(random)

        eventually { assertThat(ann.channels.unread(), equalTo(emptyMap<ChannelName, Int>())) }
    }

    @Test
    fun `everyone sees who is online, whichever channel they are in`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(random)

        eventually { assertThat(ann.online(), equalTo(setOf(ann.name, bob.name))) }

        bob.closes()

        eventually(untilAClosedTabIsNoticed) { assertThat(ann.online(), equalTo(setOf(ann.name))) }
    }

    @Test
    fun `being connected is being online, on any page`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.searches("anything")

        eventually { assertThat(ann.online(), equalTo(setOf(ann.name, bob.name))) }
    }

    @Test
    fun `leaving a channel for another page leaves the channel but stays online`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val random = ChannelName.of("random")
        ann.channels.open(random)
        bob.channels.open(random)
        eventually { assertThat(ann.online(), equalTo(setOf(ann.name, bob.name))) }

        bob.channels.searches("anything")

        eventually(untilAClosedTabIsNoticed) {
            ann.channels.open(random).posts("Still there?")
            assertThat(bob.channels.unread().keys, equalTo(setOf(random)))
        }
        assertThat(ann.online(), equalTo(setOf(ann.name, bob.name)))
    }

    @Test
    fun `teammates see who is typing, until they stop for five seconds`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general)

        bob.channels.open(general).types("Hello th")

        eventually { assertThat(ann.channels.open(general).typing(), equalTo(setOf(bob.name))) }
        assertThat(bob.channels.open(general).typing(), equalTo(emptySet<String>()))

        driver.tick(Duration.ofSeconds(6))

        eventually { assertThat(ann.channels.open(general).typing(), equalTo(emptySet<String>())) }
    }

    @Test
    fun `a teammate who has never opened a channel sees the open channels, with nothing unread`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).posts("Morning")
        val bob = driver.teammate("bob")

        assertThat(bob.channels.list(), equalTo(listOf(general, random)))
        assertThat(bob.channels.unread(), equalTo(emptyMap<ChannelName, Int>()))
    }

    @Test
    fun `a private channel is only listed for its members, and others can't read or post in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.createsPrivate(secret), equalTo(Verdict.Accepted))

        assertThat(ann.channels.list(), equalTo(listOf(general, random, secret)))
        assertThat(bob.channels.list(), equalTo(listOf(general, random)))
        assertThat(bob.channels.open(secret).canRead(), equalTo(false))
        assertThat(bob.channels.open(secret).posts("Let me in"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a member invites a teammate into a private channel, who can then read and post in it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans for the offsite")

        assertThat(ann.channels.open(secret).invites(bob), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.list(), equalTo(listOf(general, random, secret))) }
        assertThat(bob.channels.open(secret).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("Plans for the offsite"))))))
        assertThat(bob.channels.open(secret).posts("Count me in"), equalTo(Verdict.Accepted))
    }

    @Test
    fun `only members of a private channel can invite people into it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        val cat = driver.teammate("cat")
        bob.channels.open(general)
        cat.channels.open(general)
        ann.channels.createsPrivate(secret)

        assertThat(bob.channels.open(secret).invites(cat), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a public channel has nobody to invite, and can't be left`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).invites(bob), equalTo(Verdict.Denied))
        assertThat(ann.channels.open(general).leaves(), equalTo(Verdict.Denied))
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

        eventually { assertThat(bob.channels.directMessages(), equalTo(mapOf(ann.name to 1))) }
        assertThat(cat.channels.directMessages(), equalTo(emptyMap<String, Int>()))
        assertThat(cat.channels.open(dm).posts("Hi both"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a channel is headed with its name, and a direct message with the other person's`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        val dm = ann.channels.messagesDirectly(bob)

        assertThat(ann.channels.open(general).heading(), equalTo("#general"))
        assertThat(ann.channels.open(dm).heading(), equalTo(bob.name))
        assertThat(bob.channels.open(dm).heading(), equalTo(ann.name))
    }

    @Test
    fun `a mention names the teammate in the message`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("@${bob.handle} can you look at this?")

        eventually { assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("@${bob.handle} can you look at this?"), setOf(bob.name)))))) }
    }

    @Test
    fun `a mention still names the teammate inside bold text`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("*ask @${bob.handle} first*")

        eventually { assertThat(bob.channels.open(general).sees().messages.single().mentions, equalTo(setOf(bob.name))) }
    }

    @Test
    fun `search finds messages across channels, newest first, whatever the case`() {
        val ann = driver.teammate("ann")
        val random = ChannelName.of("random")
        ann.channels.open(general).posts("Lunch at noon?")
        driver.tick(Duration.ofSeconds(1))
        ann.channels.open(random).posts("who wants lunch")
        ann.channels.open(random).posts("something else")

        assertThat(ann.channels.searches("LUNCH"), equalTo(listOf(Found("#random", ann.name, MessageText.of("who wants lunch")), Found("#general", ann.name, MessageText.of("Lunch at noon?")))))
    }

    @Test
    fun `search finds replies in threads`() {
        val ann = driver.teammate("ann")
        val lunch = MessageText.of("Lunch?")
        ann.channels.open(general).posts(lunch.value)
        ann.channels.open(general).message(lunch).replies("Tacos please")

        assertThat(ann.channels.searches("tacos").map { it.text }, equalTo(listOf(MessageText.of("Tacos please"))))
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

        assertThat(ann.channels.searches("launch").map { it.text }.sortedBy { it.value }, equalTo(listOf(MessageText.of("launch is close"), MessageText.of("the launch date"))))
        assertThat(bob.channels.searches("launch"), equalTo(listOf(Found(ann.name, ann.name, MessageText.of("launch is close")))))
    }

    @Test
    fun `search results stay live`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        assertThat(bob.channels.searches("lunch"), equalTo(emptyList<Found>()))

        ann.channels.open(general).posts("Lunch?")
        eventually { assertThat(bob.channels.searches("lunch").map { it.text }, equalTo(listOf(MessageText.of("Lunch?")))) }

        ann.channels.open(general).message(MessageText.of("Lunch?")).edits("Dinner?")
        eventually { assertThat(bob.channels.searches("lunch"), equalTo(emptyList<Found>())) }
    }

    @Test
    fun `being removed from a channel takes its messages out of your results while you watch`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        ann.channels.open(secret).posts("the launch date")
        assertThat(bob.channels.searches("launch").size, equalTo(1))

        ann.channels.open(secret).removes(bob)

        eventually { assertThat(bob.channels.searches("launch"), equalTo(emptyList<Found>())) }
    }

    @Test
    fun `search doesn't find deleted messages, and a blank search finds nothing`() {
        val ann = driver.teammate("ann")
        val lunch = MessageText.of("Lunch?")
        ann.channels.open(general).posts(lunch.value)
        ann.channels.open(general).message(lunch).deletes()

        assertThat(ann.channels.searches("lunch"), equalTo(emptyList<Found>()))
        ann.channels.open(general).posts("Lunch again?")
        assertThat(ann.channels.searches(""), equalTo(emptyList<Found>()))
    }

    @Test
    fun `a link's preview appears for everyone watching, after the message`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        ann.channels.open(general).posts("read https://example.test/article please")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(bob.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("An article", "All about it"))) }
    }

    @Test
    fun `each link in a message gets its own preview, in order`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/plain and https://example.test/article")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().previews, equalTo(listOf(LinkPreview("A plain page", "Nothing fancy"), LinkPreview("An article", "All about it")))) }
    }

    @Test
    fun `a preview shows the page's image from our own server`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/pictured")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("Pictured", "", true))) }
    }

    @Test
    fun `a page with no open graph tags previews its title and description`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/plain")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("A plain page", "Nothing fancy"))) }
    }

    @Test
    fun `a preview follows a redirect`() {
        val ann = driver.teammate("ann")

        ann.channels.open(general).posts("https://example.test/moved")
        driver.tick(Duration.ofSeconds(2))

        eventually { assertThat(ann.channels.open(general).sees().messages.single().preview, equalTo(LinkPreview("An article", "All about it"))) }
    }

    @Test
    fun `editing a link away removes its preview, and editing one in adds it`() {
        val ann = driver.teammate("ann").channels.open(general)
        ann.posts("https://example.test/article")
        driver.tick(Duration.ofSeconds(2))
        eventually { assertThat(ann.sees().messages.single().preview?.title, equalTo("An article")) }

        ann.message(MessageText.of("https://example.test/article")).edits("https://example.test/plain")
        driver.tick(Duration.ofSeconds(2))
        eventually { assertThat(ann.sees().messages.single().preview?.title, equalTo("A plain page")) }

        ann.message(MessageText.of("https://example.test/plain")).edits("no link now")
        eventually { assertThat(ann.sees().messages.single().preview, equalTo(null)) }
    }

    @Test
    fun `deleting a message removes its preview`() {
        val ann = driver.teammate("ann").channels.open(general)
        val link = MessageText.of("https://example.test/article")
        ann.posts(link.value)
        driver.tick(Duration.ofSeconds(2))
        eventually { assertThat(ann.sees().messages.single().preview?.title, equalTo("An article")) }

        ann.message(link).replies("kept alive by this reply")
        ann.message(link).deletes()

        eventually { assertThat(ann.sees().messages.single().preview, equalTo(null)) }
    }

    @Test
    fun `an @ that matches nobody who can see the channel is just text`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)

        ann.channels.open(general).posts("@nobody-here hello")
        ann.channels.open(secret).posts("@${bob.handle} you can't see this")

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("@nobody-here hello"))))))
        assertThat(ann.channels.open(secret).sees(), equalTo(ChannelSeen(listOf(Said(ann.name, MessageText.of("@${bob.handle} you can't see this"))))))
    }

    @Test
    fun `unread mentions are counted in the sidebar until the channel is read`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)

        ann.channels.open(general).posts("Morning all")
        ann.channels.open(general).posts("@${bob.handle} can you look at this?")

        eventually { assertThat(bob.channels.mentions(), equalTo(mapOf(general to 1))) }
        assertThat(bob.channels.unread(), equalTo(mapOf(general to 2)))

        bob.channels.open(general)

        eventually { assertThat(bob.channels.mentions(), equalTo(emptyMap<ChannelName, Int>())) }
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

        assertThat(ann.channels.open(secret).suggestions("thoughts @"), equalTo(setOf(bob.name)))
        assertThat(ann.channels.open(general).suggestions("thoughts @"), equalTo(setOf(bob.name, cat.name)))
        assertThat(ann.channels.open(general).suggestions("thoughts @${cat.handle.take(3)}"), equalTo(setOf(cat.name)))
    }

    @Test
    fun `tab completes the handle being typed`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertThat(ann.channels.open(secret).tabs("thoughts @${bob.handle.take(2)}"), equalTo("thoughts @${bob.handle} "))
    }

    @Test
    fun `replies show in the thread for everyone looking at it, in order, and not in the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general).message(lunch).opensThread()

        assertThat(ann.channels.open(general).message(lunch).replies("Tacos"), equalTo(Verdict.Accepted))
        assertThat(bob.channels.open(general).message(lunch).replies("Seconded"), equalTo(Verdict.Accepted))

        eventually {
            assertThat(bob.channels.open(general).message(lunch).thread(), equalTo(ThreadSeen(Said(ann.name, lunch, replies = 2), listOf(Said(ann.name, tacos), Said(bob.name, MessageText.of("Seconded"))))))
        }
        assertThat(ann.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, lunch, replies = 2))))
    }

    @Test
    fun `everyone in the channel sees how many replies a message has`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general)

        ann.channels.open(general).message(lunch).replies("Tacos")

        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, lunch, replies = 1)))) }
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

        assertThat(bob.channels.open(general).message(tacos).replies("Burritos"), equalTo(Verdict.Rejected("Threads are one level deep")))
        assertThat(bob.channels.open(general).message(lunch).replies("   "), equalTo(Verdict.Rejected("Say something first")))
        assertThat(bob.channels.open(secret).message(plans).replies("Me too"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a reaction shows its count for everyone in the channel, marked as yours only for you`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(shippedIt).reacts(Emoji.tada), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1))))) }
        assertThat(ann.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1), mine = setOf(Emoji.tada)))))
    }

    @Test
    fun `the same reaction from two teammates counts twice, and reacting again takes yours back`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).reacts(Emoji.tada)
        bob.channels.open(general).message(shippedIt).reacts(Emoji.tada)
        eventually { assertThat(ann.channels.open(general).sees().messages.single().reactions, equalTo(mapOf(Emoji.tada to 2))) }

        bob.channels.open(general).message(shippedIt).reacts(Emoji.tada)

        eventually { assertThat(ann.channels.open(general).sees().messages.single(), equalTo(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1), mine = setOf(Emoji.tada)))) }
        assertThat(bob.channels.open(general).sees().messages.single(), equalTo(Said(ann.name, shippedIt, reactions = mapOf(Emoji.tada to 1))))
    }

    @Test
    fun `replies in a thread take reactions too`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        ann.channels.open(general).message(lunch).replies("Tacos")
        bob.channels.open(general).message(lunch).opensThread()

        ann.channels.open(general).message(tacos).reacts(Emoji.heart)

        eventually { assertThat(bob.channels.open(general).message(lunch).thread().replies, equalTo(listOf(Said(ann.name, tacos, reactions = mapOf(Emoji.heart to 1))))) }
    }

    @Test
    fun `reacting where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertThat(bob.channels.open(secret).message(plans).reacts(Emoji.eyes), equalTo(Verdict.Hidden))
    }

    @Test
    fun `an author's edit shows for everyone, marked as edited`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch at 12")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(MessageText.of("Lunch at 12")).edits(to = "Lunch at 1"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(Said(ann.name, MessageText.of("Lunch at 1"), edited = true)))) }
    }

    @Test
    fun `a deleted message leaves everyone's history`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Wrong channel, sorry")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(MessageText.of("Wrong channel, sorry")).deletes(), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).sees(), equalTo(ChannelSeen(emptyList()))) }
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
            assertThat(bob.channels.unread().keys, equalTo(setOf(general)))
        }
        ann.channels.open(general).posts("Lunch")

        assertThat(bob.channels.open(general).firstUnread(), equalTo("Standup"))
    }

    @Test
    fun `the divider is gone once you have read the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Morning")
        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 1))) }

        bob.channels.open(general)

        eventually(untilAClosedTabIsNoticed) {
            bob.channels.open(random)
            assertThat(bob.channels.open(general).firstUnread(), equalTo(null))
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
        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 1))) }

        assertThat(bob.channels.open(general).firstUnread(), equalTo("Morning"))
    }

    @Test
    fun `deleting a message someone hasn't read takes it off their unread count and their mentions`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("@${bob.handle} wrong person")
        ann.channels.open(general).posts("Morning all")
        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 2))) }
        assertThat(bob.channels.mentions(), equalTo(mapOf(general to 1)))

        ann.channels.open(general).message(MessageText.of("@${bob.handle} wrong person")).deletes()

        eventually { assertThat(bob.channels.unread(), equalTo(mapOf(general to 1))) }
        assertThat(bob.channels.mentions(), equalTo(emptyMap<ChannelName, Int>()))
    }

    @Test
    fun `editing a mention in counts it for the teammate mentioned`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(random)
        ann.channels.open(general).posts("Have a look")

        ann.channels.open(general).message(MessageText.of("Have a look")).edits(to = "@${bob.handle} have a look")

        eventually { assertThat(bob.channels.mentions(), equalTo(mapOf(general to 1))) }
    }

    @Test
    fun `only the author can edit or delete a message, and an edit can't blank it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.open(general).posts("Mine")

        assertThat(bob.channels.open(general).message(mine).edits(to = "Yours"), equalTo(Verdict.Denied))
        assertThat(bob.channels.open(general).message(mine).deletes(), equalTo(Verdict.Denied))
        assertThat(ann.channels.open(general).message(mine).edits(to = "   "), equalTo(Verdict.Rejected("Say something first")))
    }

    @Test
    fun `a deleted message with replies stays, so its thread still makes sense`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Lunch?")
        bob.channels.open(general).message(lunch).replies("Tacos")

        ann.channels.open(general).message(lunch).deletes()

        val tombstone = Said(ann.name, MessageText.of("This message was deleted"), replies = 1, deleted = true)
        eventually { assertThat(bob.channels.open(general).sees().messages, equalTo(listOf(tombstone))) }
        assertThat(bob.channels.open(general).message(lunch).thread(), equalTo(ThreadSeen(tombstone, listOf(Said(bob.name, tacos)))))
    }

    @Test
    fun `a member who leaves a private channel loses it, and can't read it any more`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)

        assertThat(bob.channels.open(secret).leaves(), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.list(), equalTo(listOf(general, random))) }
        assertThat(bob.channels.open(secret).canRead(), equalTo(false))
    }

    @Test
    fun `the owner removes a teammate who is watching, and they land in general without the channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).invites(bob)
        bob.channels.open(secret)

        assertThat(ann.channels.open(secret).removes(bob), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.viewing(), equalTo(general)) }
        eventually { assertThat(bob.channels.list(), equalTo(listOf(general, random))) }
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

        assertThat(bob.channels.open(secret).removes(cat), equalTo(Verdict.Denied))
        assertThat(ann.channels.open(dm).leaves(), equalTo(Verdict.Denied))
    }

    @Test
    fun `the last member leaving deletes the channel and everything in it`() {
        val ann = driver.teammate("ann")
        ann.channels.createsPrivate(solo)
        ann.channels.open(solo).posts("Mine")
        ann.channels.open(solo).topic.sets("Just me")
        ann.channels.open(solo).bookmarks.adds("Runbook", "https://example.com/runbook")
        ann.channels.open(solo).canvas.saves("Notes")

        assertThat(ann.channels.open(solo).leaves(), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.list(), equalTo(listOf(general, random))) }

        ann.channels.createsPrivate(solo)

        assertThat(ann.channels.open(solo).sees(), equalTo(ChannelSeen(emptyList())))
        assertThat(ann.channels.open(solo).topic.read(), equalTo(""))
        assertThat(ann.channels.open(solo).bookmarks.list(), equalTo(emptyList<Bookmarked>()))
        assertThat(ann.channels.open(solo).canvas.read(), equalTo(""))
    }

    @Test
    fun `a file sent to a channel shows for everyone and downloads as it was sent`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).attaches(notes, "Shipped it"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).message(shippedIt).attachments(), equalTo(listOf(Attached("notes.txt", inline = false)))) }
        assertThat(bob.channels.open(general).downloads("notes.txt"), equalTo(Downloaded(notes.contents, "application/octet-stream", inline = false)))
    }

    @Test
    fun `an image shows inline, and a file with nothing to say goes under its own name`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.open(general).attaches(photo), equalTo(Verdict.Accepted))

        assertThat(ann.channels.open(general).message(MessageText.of("photo.png")).attachments(), equalTo(listOf(Attached("photo.png", inline = true))))
        assertThat(ann.channels.open(general).downloads("photo.png"), equalTo(Downloaded(photo.contents, "image/png", inline = true)))
    }

    @Test
    fun `a file that could run in the page never shows inline, whatever it claims to be`() {
        val ann = driver.teammate("ann")
        val script = Base64Blob.encode("<script>alert(1)</script>")

        ann.channels.open(general).attaches(FileToSend("page.html", "text/html", script), "Plans")
        ann.channels.open(general).attaches(FileToSend("logo.svg", "image/svg+xml", script), "Mine")

        assertThat(ann.channels.open(general).message(plans).attachments(), equalTo(listOf(Attached("page.html", inline = false))))
        assertThat(ann.channels.open(general).message(mine).attachments(), equalTo(listOf(Attached("logo.svg", inline = false))))
        assertThat(ann.channels.open(general).downloads("logo.svg"), equalTo(Downloaded(script, "application/octet-stream", inline = false)))
    }

    @Test
    fun `several files go with one message, by name whatever order they arrive in`() {
        val ann = driver.teammate("ann")
        val files = (1..5).map { FileToSend("file$it.txt", "text/plain", Base64Blob.encode("Number $it")) }

        assertThat(ann.channels.open(general).attachesAll(files.reversed(), "All of them"), equalTo(Verdict.Accepted))

        assertThat(ann.channels.open(general).message(MessageText.of("All of them")).attachments(), equalTo(files.map { Attached(it.name, inline = false) }))
    }

    @Test
    fun `a message with several files and nothing to say goes under the first by name`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.open(general).attachesAll(listOf(photo, notes)), equalTo(Verdict.Accepted))

        assertThat(ann.channels.open(general).sees().messages.map { it.text.value }, equalTo(listOf("notes.txt")))
    }

    @Test
    fun `more than ten files at once are refused, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val files = (1..11).map { FileToSend("file$it.txt", "text/plain", Base64Blob.encode("Number $it")) }

        assertThat(ann.channels.open(general).attachesAll(files, "Too many"), equalTo(Verdict.Rejected("Send up to 10 files at once")))

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `one bad file refuses them all, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val huge = FileToSend("huge.bin", "application/octet-stream", Base64Blob.encode(ByteArray(5 * 1024 * 1024 + 1)))

        assertThat(ann.channels.open(general).attachesAll(listOf(notes, huge), "Shipped it"), equalTo(Verdict.Rejected("Keep files to 5 MB")))

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(emptyList())))
    }

    @Test
    fun `a file over five megabytes is refused, and nothing is posted`() {
        val ann = driver.teammate("ann")
        val huge = FileToSend("huge.bin", "application/octet-stream", Base64Blob.encode(ByteArray(5 * 1024 * 1024 + 1)))

        assertThat(ann.channels.open(general).attaches(huge, "Shipped it"), equalTo(Verdict.Rejected("Keep files to 5 MB")))

        assertThat(ann.channels.open(general).sees(), equalTo(ChannelSeen(emptyList())))
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

        assertThat(bob.channels.open(secret).downloads("notes.txt")?.contents, equalTo(notes.contents))
        assertThat(cat.channels.open(secret).attaches(photo), equalTo(Verdict.Hidden))
        assertThat(cat.channels.open(secret).downloads("notes.txt"), equalTo(null))

        ann.channels.open(secret).removes(bob)

        eventually { assertThat(bob.channels.open(secret).downloads("notes.txt"), equalTo(null)) }
    }

    @Test
    fun `deleting a message takes its file with it`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).attaches(notes, "Plans")

        ann.channels.open(general).message(plans).deletes()

        assertThat(ann.channels.open(general).downloads("notes.txt"), equalTo(null))
    }

    @Test
    fun `a channel's topic shows for everyone in it, and only in that channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).topic.sets("Shipping on Friday"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).topic.read(), equalTo("Shipping on Friday")) }
        assertThat(bob.channels.open(random).topic.read(), equalTo(""))
    }

    @Test
    fun `a blank topic clears it, and a long one is refused`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).topic.sets("Shipping on Friday")

        assertThat(ann.channels.open(general).topic.sets("x".repeat(251)), equalTo(Verdict.Rejected("Keep the topic to 250 characters")))
        assertThat(ann.channels.open(general).topic.read(), equalTo("Shipping on Friday"))

        assertThat(ann.channels.open(general).topic.sets("   "), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).topic.read(), equalTo(""))
    }

    @Test
    fun `setting the topic where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)

        assertThat(bob.channels.open(secret).topic.sets("Peeking"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `bookmarks show for everyone in the channel, in the order they were added`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).bookmarks.adds("Runbook", "https://example.com/runbook"), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).bookmarks.adds("Dashboard", "http://example.com/dash?team=acme&view=1"), equalTo(Verdict.Accepted))

        eventually {
            assertThat(bob.channels.open(general).bookmarks.list(), equalTo(listOf(Bookmarked("Runbook", "https://example.com/runbook"), Bookmarked("Dashboard", "http://example.com/dash?team=acme&view=1"))))
        }
        assertThat(bob.channels.open(random).bookmarks.list(), equalTo(emptyList<Bookmarked>()))
    }

    @Test
    fun `anyone in the channel can remove a bookmark`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).bookmarks.adds("Runbook", "https://example.com/runbook")
        ann.channels.open(general)

        assertThat(bob.channels.open(general).bookmarks.removes("Runbook"), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).bookmarks.list(), equalTo(emptyList<Bookmarked>())) }
    }

    @Test
    fun `a bookmark needs a title and a web link`() {
        val ann = driver.teammate("ann")

        assertThat(ann.channels.open(general).bookmarks.adds("  ", "example.com"), equalTo(Verdict.Rejected("Give the bookmark a title")))
        assertThat(ann.channels.open(general).bookmarks.adds("Sneaky", "javascript:alert(1)"), equalTo(Verdict.Rejected("Use an http or https link")))
        assertThat(ann.channels.open(general).bookmarks.adds("Files", "ftp://example.com/x"), equalTo(Verdict.Rejected("Use an http or https link")))
        assertThat(ann.channels.open(general).bookmarks.list(), equalTo(emptyList<Bookmarked>()))
    }

    @Test
    fun `changing bookmarks where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).bookmarks.adds("Plans", "https://example.com/plans")

        assertThat(bob.channels.open(secret).bookmarks.adds("Peeking", "https://example.com/peek"), equalTo(Verdict.Hidden))
        assertThat(bob.channels.open(secret).bookmarks.removes("Plans"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a pinned message shows at the top for everyone until someone unpins it`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).posts("Morning all")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).message(shippedIt).togglesPin(), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).pinned(), equalTo(listOf(Pinned(ann.name, "Shipped it")))) }
        assertThat(bob.channels.open(random).pinned(), equalTo(emptyList<Pinned>()))

        assertThat(bob.channels.open(general).message(shippedIt).togglesPin(), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).pinned(), equalTo(emptyList<Pinned>())) }
    }

    @Test
    fun `clicking the pin at the top of the channel unpins the message for everyone`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).togglesPin()
        ann.channels.open(general)

        assertThat(bob.channels.open(general).message(shippedIt).unpinsFromTheTop(), equalTo(Verdict.Accepted))

        eventually { assertThat(ann.channels.open(general).pinned(), equalTo(emptyList<Pinned>())) }
    }

    @Test
    fun `a pin follows its message's edits, and goes when the message is deleted`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).posts("Shipped it")
        ann.channels.open(general).message(shippedIt).togglesPin()
        bob.channels.open(general)

        ann.channels.open(general).message(shippedIt).edits("Shipped it twice")

        eventually { assertThat(bob.channels.open(general).pinned(), equalTo(listOf(Pinned(ann.name, "Shipped it twice")))) }

        ann.channels.open(general).message(MessageText.of("Shipped it twice")).deletes()

        eventually { assertThat(bob.channels.open(general).pinned(), equalTo(emptyList<Pinned>())) }
    }

    @Test
    fun `pinning where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)
        ann.channels.open(secret).posts("Plans")

        assertThat(bob.channels.open(secret).message(plans).togglesPin(), equalTo(Verdict.Hidden))
    }

    @Test
    fun `a saved canvas shows for everyone in the channel, and only in that channel`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        bob.channels.open(general)

        assertThat(ann.channels.open(general).canvas.saves("Release checklist\n- tag\n- deploy"), equalTo(Verdict.Accepted))

        eventually { assertThat(bob.channels.open(general).canvas.read(), equalTo("Release checklist\n- tag\n- deploy")) }
        assertThat(bob.channels.open(random).canvas.read(), equalTo(""))
    }

    @Test
    fun `a teammate picks up where the last save left off`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).canvas.saves("First draft")

        assertThat(bob.channels.open(general).canvas.saves("Second draft"), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).canvas.saves("Third draft"), equalTo(Verdict.Accepted))

        assertThat(bob.channels.open(general).canvas.read(), equalTo("Third draft"))
    }

    @Test
    fun `saving over a teammate's newer save is refused, and their text survives`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general).canvas.saves("First draft")
        ann.channels.open(general).canvas.begins()
        bob.channels.open(general).canvas.saves("Bob's draft")

        assertThat(ann.channels.open(general).canvas.saves("Ann's draft"), equalTo(Verdict.Rejected("A teammate saved first. Copy your text, then edit again")))
        assertThat(bob.channels.open(general).canvas.read(), equalTo("Bob's draft"))

        ann.channels.open(general).canvas.begins()

        assertThat(ann.channels.open(general).canvas.saves("Ann's draft"), equalTo(Verdict.Accepted))
        assertThat(bob.channels.open(general).canvas.read(), equalTo("Ann's draft"))
    }

    @Test
    fun `a blank canvas clears it, and an enormous one is refused`() {
        val ann = driver.teammate("ann")
        ann.channels.open(general).canvas.saves("First draft")

        assertThat(ann.channels.open(general).canvas.saves("x".repeat(20001)), equalTo(Verdict.Rejected("Keep the canvas to 20000 characters")))
        assertThat(ann.channels.open(general).canvas.read(), equalTo("First draft"))

        ann.channels.open(general).canvas.begins()

        assertThat(ann.channels.open(general).canvas.saves(""), equalTo(Verdict.Accepted))
        assertThat(ann.channels.open(general).canvas.read(), equalTo(""))
    }

    @Test
    fun `saving a canvas where you can't see the channel is denied`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.createsPrivate(secret)

        assertThat(bob.channels.open(secret).canvas.saves("Peeking"), equalTo(Verdict.Hidden))
    }

    @Test
    fun `each teammate sees which person in the list is them`() {
        val ann = driver.teammate("ann")
        val bob = driver.teammate("bob")
        ann.channels.open(general)
        bob.channels.open(general)

        eventually { assertThat(ann.you(), equalTo(ann.name)) }
        eventually { assertThat(bob.you(), equalTo(bob.name)) }
    }
}