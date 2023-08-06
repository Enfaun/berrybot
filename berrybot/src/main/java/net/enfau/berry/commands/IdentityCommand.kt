package net.enfau.berry.commands

import net.enfau.berry.dao.*
import net.enfau.berry.utils.base32ToAuthorId
import net.enfau.berry.utils.toBase32
import net.enfau.berry.utils.toBase64
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.db.DatabaseComponent
import org.briarproject.bramble.api.db.DbException
import org.briarproject.bramble.api.db.NoSuchContactException
import org.briarproject.bramble.api.db.TransactionManager
import org.briarproject.bramble.api.event.EventBus
import org.briarproject.bramble.api.event.EventExecutor
import org.briarproject.bramble.api.identity.AuthorId
import org.briarproject.bramble.api.identity.IdentityManager
import org.briarproject.bramble.api.mailbox.MailboxConstants
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.sync.MessageId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.bramble.api.system.TaskScheduler
import org.briarproject.briar.api.conversation.ConversationManager
import org.briarproject.briar.api.forum.ForumManager
import org.briarproject.briar.api.forum.ForumSharingManager
import org.briarproject.briar.api.identity.AuthorManager
import org.briarproject.briar.api.introduction.IntroductionManager
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import org.briarproject.briar.api.messaging.PrivateMessageHeader
import org.briarproject.briar.api.messaging.event.PrivateMessageReceivedEvent
import java.util.*
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.random.Random

@Immutable
@Singleton
class IdentityCommand
@Inject
constructor(
    messagingManager: MessagingManager,
    privateMessageFactory: PrivateMessageFactory,
    clock: Clock,
    private val authorManager: AuthorManager,
    private val contactManager: ContactManager,
    private val introductionManager: IntroductionManager,
    private val identityManager: IdentityManager,
    private val forumManager: ForumManager,
    private val forumSharingManager: ForumSharingManager,
    private val conversationManager: ConversationManager,
    private val transactionManager: TransactionManager,
    private val databaseComponent: DatabaseComponent,
    private val taskScheduler: TaskScheduler,
    @EventExecutor private val executor: Executor,
    private val permissionDao: PermissionDao,
    private val contactDao: ContactDao,
    private val aliasDao: AliasDao,
    private val variableDao: VariableDao,
) : BotCommand(messagingManager, privateMessageFactory, clock) {
    private val admins = ArrayList<AuthorId>()

    @DewCommand("queryuser", privileged = true)
    fun queryuser(query: String, groupId: GroupId, contactId: ContactId) {
        val perm = permissionDao.get(contactManager.getContact(contactId).author.id)
        if(!perm.has(PermissionFlag.QUERY_USER)) return
        val contacts = contactDao.findContactsByName(query)
        var msg = "Result:"
        for(contact in contacts) {
            msg += "\n#${contact.sid} ${contact.name}\n${contact.authorIdStr}\n"

        }
        sendMessage(groupId, msg.trim())
    }


    @DewCommand("lookup", shortHelp = "Lookup an alias", longHelp = "Synopsis:\n/lookup ALIAS")
    fun lookup(alias: String, groupId: GroupId, contactId: ContactId) {
        if (alias.length < 3 + 5) {
            sendMessage(groupId, "Target alias is too short")
            return
        }
        val author = aliasDao.findAuthor(alias)
        if (author == null) {
            sendMessage(groupId, "User not found")
            return
        }
        val remote = author.authorId.base32ToAuthorId()
        val local = identityManager.localAuthor.id
        try {
            val currentUserContact = contactManager.getContact(contactId)
            val result = contactManager.getContact(remote, local)
            introductionManager.makeIntroduction(currentUserContact, result, "Introduction request")
        } catch (e: NoSuchContactException) {
            sendMessage(groupId, "User not found")
        }
    }

    @DewCommand("rmalias", shortHelp = "Remove configured alias")
    fun deletealias(groupId: GroupId, contactId: ContactId) {
        val contact = contactManager.getContact(contactId)
        val author = contact.author
        val authorId = author.id.toBase32()
        if (aliasDao.getAlias(authorId) == null) {
            sendMessage(groupId, "You haven't set an alias yet")
            return
        }
        aliasDao.update(authorId, "")
        sendMessage(groupId, "Alias removed successfully")

    }

    @DewCommand(
        "setalias",
        shortHelp = "Set alias",
        longHelp = "Synopsis:\n/setalias ALIAS\n\nYour alias will be appended with hash sign along with four numbers"
    )
    fun setalias(inputAlias: String, groupId: GroupId, contactId: ContactId) {
        val rand = Random(clock.currentTimeMillis()).nextInt(1000, 9999)
        if (!Regex("^[A-Za-z0-9_\\-]{3,}$").matches(inputAlias)) {
            sendMessage(groupId, "Your alias can only contain alphanumeric characters, dash and underline and has to be at least 3 characters long.")
            return
        }
        val alias = "$inputAlias#${rand}"
        val contact = contactManager.getContact(contactId)
        val author = contact.author
        val authorId = author.id

        if (aliasDao.getAlias(authorId) == null) {
            aliasDao.insert(authorId, alias)
        } else {
            aliasDao.update(authorId, alias)
        }
        sendMessage(groupId, "Your alias has been set to $alias")

    }


    @DewCommand("disconnect", shortHelp = "Disconnect bot from you")
    fun disconnect(arg: String, groupId: GroupId, contactId: ContactId, h: PrivateMessageHeader) {
        if(arg=="") {
            sendMessage(groupId, "Do you want to disconnect from the bot?, to confirm, type /disconnect Yes!")
            return
        }
        if(arg!="Yes!") {
            sendMessage(groupId, "Please type Yes! with capitalized Y and exclamation mark to confirm disconnect.\n" +
                    "eg. /disconnect Yes!")
            return
        }
        val contact = contactManager.getContact(contactId)
        val authorId = contact.author.id
        if (aliasDao.getAlias(authorId) != null)
            aliasDao.update(authorId, "")
        val link = variableDao.getOrDefault("landingLink", "(link not set)")
        var delay = clock.currentTimeMillis() - h.timestamp
        if(delay<0) delay = 0
        sendMessage(groupId, "Contact is scheduled for disconnection.\n" +
                "To connect to the bot again, submit your Briar link at $link")
        val runnable = Runnable {
            contactManager.removeContact(contactId)
        }
        taskScheduler.schedule(runnable, executor, 100000+delay*1.5.toLong(), TimeUnit.MILLISECONDS)
    }
}
