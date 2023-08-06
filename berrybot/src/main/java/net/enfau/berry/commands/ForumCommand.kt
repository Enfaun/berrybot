package net.enfau.berry.commands

import net.enfau.berry.dao.PermissionDao
import net.enfau.berry.dao.PermissionFlag
import net.enfau.berry.utils.base32Decode
import net.enfau.berry.utils.toBase32
import net.enfau.berry.utils.toGroupId
import org.briarproject.bramble.api.FormatException
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.db.DatabaseComponent
import org.briarproject.bramble.api.db.DbException
import org.briarproject.bramble.api.db.NoSuchGroupException
import org.briarproject.bramble.api.db.TransactionManager
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.bramble.util.StringUtils
import org.briarproject.briar.api.forum.Forum
import org.briarproject.briar.api.forum.ForumManager
import org.briarproject.briar.api.forum.ForumSharingManager
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil

@Immutable
@Singleton
class ForumCommand
@Inject
constructor(
    messagingManager: MessagingManager,
    privateMessageFactory: PrivateMessageFactory,
    clock: Clock,
    private val contactManager: ContactManager,
    private val forumManager: ForumManager,
    private val forumSharingManager: ForumSharingManager,
    private val transactionManager: TransactionManager,
    private val databaseComponent: DatabaseComponent,
    private val permissionDao: PermissionDao
) : BotCommand(messagingManager, privateMessageFactory, clock) {

    @DewCommand("list", shortHelp = "List forums", longHelp = "Synopsis:\n/list [page_number]")
    fun list(arg: String, groupId: GroupId) {
        var page = 0
        val size = 7
        try {
            page = Integer.parseInt(arg)-1
        } catch (e: NumberFormatException) {}
        var forums = transactionManager.transactionWithResult<Collection<Forum>, DbException>(true) { txn -> forumManager.getForums(txn)}
        val pages = ceil(forums.size.toDouble()/size).toInt()
        if(page>=pages) page = pages-1
        var floor = page*size
        var ceil = (page+1)*size
        if(ceil>forums.size) ceil = forums.size
        forums = forums.toList().subList(floor, ceil)
        var body = "Forum listing (page ${page+1}, total $pages page(s)):"

        for(forum in forums) {
            val count = forumSharingManager.getSharedWith(forum.id).size
            body += "\n\n${forum.name} ($count member(s))\n${forum.id.toBase32()}"

        }
        sendMessage(groupId, body)
    }

    @DewCommand("leave", privileged = true, shortHelp = "Leave a forum")
    fun leave(arg: String, groupId: GroupId, contactId: ContactId) {

        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.REMOVE_FORUM)
        if(!authed) {
            sendMessage(groupId, "You don't have permission to do this.")
            return
        }
        try {
            val forumGroupId = arg.base32Decode().toGroupId()
            val forum = forumManager.getForum(forumGroupId)
            forumManager.removeForum(forum)
        } catch (e: FormatException) {
            sendMessage(groupId, "Invalid ID")
        }
    }

    @DewCommand("join", shortHelp = "Join forum", longHelp = "Synopsis:\n/join room_id")
    fun join(arg: String, groupId: GroupId, contactId: ContactId) {

        try {
            arg.base32Decode().toGroupId()
        } catch (e: FormatException) {
            sendMessage(groupId, "Invalid ID")
            return
        }
        try {
            val forumGroupId = arg.base32Decode().toGroupId()
            val forum = forumManager.getForum(forumGroupId)

//            var allInvitations = databaseComponent.transactionWithResult<Collection<ConversationMessageHeader>, DbException>(true) { txn ->
//                forumSharingManager.getMessageHeaders(txn, contactId)
//
//            }
//            for(invite in allInvitations) {
//                when(invite){
//                    is ForumInvitationRequest -> {
//                        val invite: ForumInvitationRequest = invite
//                        if(!invite.wasAnswered()) {
//                            forumSharingManager.respondToInvitation(contactId, invite.sessionId, false)
//                        }
//
//                    }
//                }
//            }


            forumSharingManager.sendInvitation(forumGroupId, contactId, "Forum: ${forum.name}")
        }catch (e: NoSuchGroupException) {
            sendMessage(groupId, "No such forum")
            return
        } catch (e: FormatException) {
            sendMessage(groupId, "No such forum")
            return
        }
    }
}
