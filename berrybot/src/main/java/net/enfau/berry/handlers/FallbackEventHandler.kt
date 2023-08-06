package net.enfau.berry.handlers

import net.enfau.berry.dao.ContactDao
import net.enfau.berry.utils.WithLogger
import org.briarproject.bramble.api.client.ClientHelper
import org.briarproject.bramble.api.connection.ConnectionRegistry
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.db.*
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.mailbox.MailboxConstants.MAX_FILE_PAYLOAD_BYTES
import org.briarproject.bramble.api.mailbox.MailboxConstants.MAX_LATENCY
import org.briarproject.bramble.api.mailbox.MailboxManager
import org.briarproject.bramble.api.plugin.event.ConnectionOpenedEvent
import org.briarproject.bramble.api.plugin.event.TransportActiveEvent
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.sync.MessageId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.attachment.AttachmentReader
import org.briarproject.briar.api.autodelete.AutoDeleteManager
import org.briarproject.briar.api.avatar.AvatarManager
import org.briarproject.briar.api.blog.BlogManager
import org.briarproject.briar.api.conversation.ConversationManager
import org.briarproject.briar.api.conversation.ConversationMessageHeader
import org.briarproject.briar.api.conversation.DeletionResult
import org.briarproject.briar.api.forum.*
import org.briarproject.briar.api.forum.event.ForumPostReceivedEvent
import org.briarproject.briar.api.identity.AuthorManager
import org.briarproject.briar.api.introduction.IntroductionManager
import org.briarproject.briar.api.introduction.IntroductionRequest
import org.briarproject.briar.api.introduction.IntroductionResponse
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import org.briarproject.briar.api.messaging.event.PrivateMessageReceivedEvent
import org.briarproject.briar.api.privategroup.PrivateGroupManager
import org.briarproject.briar.api.privategroup.event.GroupInvitationRequestReceivedEvent
import org.briarproject.briar.api.privategroup.invitation.GroupInvitationManager
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

import org.briarproject.bramble.api.plugin.event.ContactConnectedEvent
import org.briarproject.bramble.api.sync.event.MessageToAckEvent

@Immutable
@Singleton
class FallbackEventHandler
@Inject
constructor(
    private val autoDeleteManager: AutoDeleteManager,
    private val authorManager: AuthorManager,
    private val avatarManager: AvatarManager,
    private val blogManager: BlogManager,
    private val attachmentReader: AttachmentReader,
    private val contactManager: ContactManager,
    private val conversationManager: ConversationManager,
    private val forumManager: ForumManager,
    private val forumSharingManager: ForumSharingManager,
    private val introductionManager: IntroductionManager,
    private val messagingManager: MessagingManager,
    private val mailboxManager: MailboxManager,
    private val groupInvitationManager: GroupInvitationManager,
    private val transactionManager: TransactionManager,
    private val privateGroupManager: PrivateGroupManager,
    private val privateMessageFactory: PrivateMessageFactory,
    private val connectionRegistry: ConnectionRegistry,
    private val clientHelper: ClientHelper,
    private val databaseComponent: DatabaseComponent,
    private val clock: Clock,
    private val contactDao: ContactDao
) : EventListener, WithLogger {
    private val logger = logger()


    private fun checkRequests(contactId: ContactId) {
        processIntroductions(contactId)

    }


    private fun processIntroductions(contactId: ContactId) {

        // find introduction requests
        var introductions =
            transactionManager.transactionWithResult<Collection<ConversationMessageHeader>, DbException>(true) { txn ->
                introductionManager.getMessageHeaders(
                    txn,
                    contactId
                )
            }

        if (introductions.isEmpty()) return
        val msgToRemove = HashSet<MessageId>()
        // accept all introduction requests and clean up old introductions

        for (req in introductions) {
            when (req) {
                is IntroductionRequest -> {
                    val req: IntroductionRequest = req

                    logger.debug(req.id.toString())

                    if (!req.wasAnswered())
                        introductionManager.respondToIntroduction(contactId, req.sessionId, false)

//                    msgToRemove.add(req.id)
//                    if (!req.wasAnswered()) {
//                        introductionManager.respondToIntroduction(contactId, req.sessionId, false)
//
//                    } else {
//                        logger.debug("Introduction ${req.id} is already answered")
//                    }
                }

                is IntroductionResponse -> {
//                    val req: IntroductionResponse = req
//                    logger.debug(req.id.toString())
//                    if (req.wasAccepted())
//                        msgToRemove.add(req.id)
                }

                else -> {
                    logger.warn("Unknown introduction message: ${req.javaClass.simpleName}")
                }
            }
        }
        if (msgToRemove.isEmpty()) return
        logger.debug("Processing introduction of ${contactManager.getContact(contactId).author.name}")
        var result = transactionManager.transactionWithResult<DeletionResult, DbException>(false) { txn ->
            introductionManager.deleteMessages(txn, contactId, msgToRemove.toSet())
        }

        logger.debug("Delete result: ${result.hasIntroductionSessionInProgress()} ${result.hasInvitationSessionInProgress()} ${result.hasNotAllIntroductionSelected()} ${result.hasInvitationSessionInProgress()}")

    }

    private fun dissolveGroup(groupId: GroupId) {
        val contactId = messagingManager.getContactId(groupId)
        conversationManager.deleteAllMessages(contactId)
    }

    private fun onActive() {
        // reject all invitations

//        // group feature is disabled
//        for (item in groupInvitationManager.invitations) {
//            logger.info("Rejecting previous invites")
//            groupInvitationManager.respondToInvitation(item.creator.id, item.shareable, false)
//        }
        for (item in forumSharingManager.invitations) {
            logger.info("Rejecting previous invites not responded but stored in db")
            val forum = item.shareable as Forum
            for (sharer in item.newSharers) {
                forumSharingManager.respondToInvitation(forum, sharer, false)
            }
        }

        for (i in contactManager.pendingContacts) {
            val contact = i.first
            val state = i.second
            if (clock.currentTimeMillis() - contact.timestamp > MAX_LATENCY) {
                logger.info("Removing pending contact ${contact.id}")
                contactManager.removePendingContact(contact.id)
            }
        }

    }

    private fun cleanupMessages(contactId: ContactId, groupId: GroupId) {
        var msqQueue = databaseComponent.transactionWithResult<Collection<MessageId>, DbException>(true) { txn ->
            databaseComponent.getMessagesToSend(
                txn,
                contactId,
                MAX_FILE_PAYLOAD_BYTES.toLong(),
                MAX_LATENCY
            )
        }
        var allMessages = databaseComponent.transactionWithResult<Collection<MessageId>, DbException>(true) { txn ->
            messagingManager.getMessageIds(
                txn,
                contactId
            )

        }.filter { !msqQueue.contains(it) }




//        var counter =
//        if (allMessages.size < 20) return
//        allMessages = allMessages.subList(0, allMessages.size - counter)
        logger.info("Cleaning up ${allMessages.size} messages of $contactId")

        databaseComponent.transaction<DbException>(false) { txn ->
            for (msg in allMessages) {
                    databaseComponent.removeMessage(txn, msg)
            }
        }
    }

    override fun eventOccurred(e: Event) {

        logger.debug("Received event: $e")
        when (e) {

            is TransportActiveEvent -> {
                logger.info(
                    """
    ____               __
   / __ \___ _      __/ /_  ___  ____________  __
  / / / / _ \ | /| / / __ \/ _ \/ ___/ ___/ / / /
 / /_/ /  __/ |/ |/ / /_/ /  __/ /  / /  / /_/ /
/_____/\___/|__/|__/_.___/\___/_/  /_/   \__, /
                                        /____/
Dewberry is starting!
"""
                )
                onActive()
            }

            is GroupInvitationRequestReceivedEvent -> {
                logger.info("Rejecting group invites")
                var e: GroupInvitationRequestReceivedEvent = e
                groupInvitationManager.respondToInvitation(e.contactId, e.messageHeader.sessionId, false)
                conversationManager.setReadFlag(e.messageHeader.groupId, e.messageHeader.id, true)
                val msg = privateMessageFactory.createLegacyPrivateMessage(
                    e.messageHeader.groupId,
                    clock.currentTimeMillis(),
                    "Group is not implemented"
                )
                messagingManager.addLocalMessage(msg)
            }

            is ConnectionOpenedEvent -> { // new client
                val e: ConnectionOpenedEvent = e
                val contact = contactManager.getContact(e.contactId)
                checkRequests(e.contactId)
            }


            is PrivateMessageReceivedEvent -> {
                val e: PrivateMessageReceivedEvent = e
                if(e.messageHeader.attachmentHeaders.isNotEmpty()) {
                    for(ah in e.messageHeader.attachmentHeaders) {

                    }
                }
                val contact = contactManager.getContact(e.contactId)
                if(!contactDao.exists(contact.author.id)){
                    contactDao.insert(contact.author.id, contact.author.name ,clock.currentTimeMillis())
                } else {
                    contactDao.updateLastCommunicatedAt(contact.author.id, clock.currentTimeMillis())
                }

                cleanupMessages(e.contactId, e.messageHeader.groupId)
            }
        }
    }
}
