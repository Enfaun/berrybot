package net.enfau.berry.commands

import net.enfau.berry.dao.PermissionDao
import net.enfau.berry.dao.VariableDao
import net.enfau.berry.utils.toBase64
import net.enfau.berry.utils.toBase32
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.contact.HandshakeLinkConstants
import org.briarproject.bramble.api.crypto.CryptoConstants
import org.briarproject.bramble.api.crypto.PublicKey
import org.briarproject.bramble.api.db.DatabaseComponent
import org.briarproject.bramble.api.db.DbException
import org.briarproject.bramble.api.db.TransactionManager
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.sync.MessageId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.bramble.util.Base32
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import org.briarproject.briar.api.messaging.PrivateMessageHeader
import org.briarproject.briar.api.messaging.event.PrivateMessageReceivedEvent
import java.math.BigInteger
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class UtilityCommand
@Inject
constructor(
    messagingManager: MessagingManager,
    privateMessageFactory: PrivateMessageFactory,
    clock: Clock,
    private val contactManager: ContactManager,
    private val transactionManager: TransactionManager,
    private val databaseComponent: DatabaseComponent,
    private val permissionDao: PermissionDao,
    private val variableDao: VariableDao
) : BotCommand(messagingManager, privateMessageFactory, clock) {
    @DewCommand("ping", shortHelp = "Pong!")
    fun ping(groupId: GroupId, messageHeader: PrivateMessageHeader) {
        val delay = clock.currentTimeMillis() - messageHeader.timestamp
        sendMessage(groupId, "Pong! ${delay}ms")
    }

    @DewCommand("link", shortHelp = "Get bot link")
    fun link(groupId: GroupId) {
        sendMessage(groupId, "Bot link:\n${contactManager.handshakeLink}")
    }

    private fun createHandshakeLink(k: PublicKey): String {
        require(k.keyType == CryptoConstants.KEY_TYPE_AGREEMENT)
        val encoded = k.encoded
        require(encoded.size == HandshakeLinkConstants.RAW_LINK_BYTES - 1)
        val raw = ByteArray(HandshakeLinkConstants.RAW_LINK_BYTES)
        raw[0] = HandshakeLinkConstants.FORMAT_VERSION.toByte()
        System.arraycopy(encoded, 0, raw, 1, encoded.size)
        return "briar://" + Base32.encode(raw).lowercase()
    }

    @DewCommand("info", shortHelp = "Show all information of user")
    fun info(groupId: GroupId, h: PrivateMessageHeader, e: PrivateMessageReceivedEvent) {
        val contactId = e.contactId
        val contact = contactManager.getContact(contactId)
        val author = contact.author
        val group = messagingManager.getContactGroup(contact)
        var messages =
            transactionManager.transactionWithResult<Set<MessageId>, DbException>(true) { txn ->
                messagingManager.getMessageIds(txn, contactId)
            }



        var pubkey = contact.handshakePublicKey
        var link = "N/A"
        if(pubkey!=null) link = createHandshakeLink(pubkey)
        var body = """
                        Author Name: ${contact.author.name}
                        Stored Message Count: ${messages.size}
                        Author ID(Number):
                        ${BigInteger(author.id.bytes)}
                        Author ID(Base32):
                        ${author.id.toBase32()}
                        Group ID(Number):
                        ${BigInteger(group.id.bytes)}
                        Group ID(Base32):
                        ${group.id.toBase32()}
                        Public Signature Key(Base64):
                        ${contact.author.publicKey.encoded.toBase64()}
                        Contact ID(Number):
                        ${contact.id.int}
                        Handshake Link:
                        $link
                    """.trimIndent()
        sendMessage(groupId, body)
    }

    @DewCommand("permission_flag", privileged = true)
    fun permissionFlag(groupId: GroupId, e: PrivateMessageReceivedEvent) {
        val contactId = e.contactId
        val contact = contactManager.getContact(contactId)
        val author = contact.author
        val flag = permissionDao.get(author.id).bits()
        sendMessage(groupId, flag.toString(2))
    }

    @DewCommand("about", shortHelp = "About this bot")
    fun about(groupId: GroupId) {
        var body = """
            BerryBot - A Bot for connecting people on Briar
            Written by Enfaun
            
            Information about how the data is being processed:
            This bot stores information about your Briar name, Unique ID, initial handshake keys and public keys.
            Private messages are not stored and are deleted immediately upon received.
            Forum posts will only be stored for 60 posts and less for each forum, more are truncated, this is subject to change.
            Metadata (invitations & member join/left events) are not removed to ensure database integrity.
            
            Support Forum: ${variableDao.getOrDefault("supportForum", "N/A")}
        """.trimIndent()
        sendMessage(groupId, body)
    }
}
