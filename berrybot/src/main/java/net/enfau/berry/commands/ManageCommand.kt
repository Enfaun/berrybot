package net.enfau.berry.commands

import net.enfau.berry.DewConstants
import net.enfau.berry.dao.PermissionDao
import net.enfau.berry.dao.PermissionFlag
import net.enfau.berry.dao.VariableDao
import net.enfau.berry.utils.base32Decode
import net.enfau.berry.utils.toBase32
import net.enfau.berry.utils.toGroupId
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.db.DatabaseComponent
import org.briarproject.bramble.api.db.DbException
import org.briarproject.bramble.api.db.PendingContactExistsException
import org.briarproject.bramble.api.sync.Group.Visibility
import org.briarproject.bramble.api.sync.GroupFactory
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.sync.SyncSession
import org.briarproject.bramble.api.sync.SyncSessionFactory
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.forum.ForumSharingManager
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import java.lang.System.setProperty
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton

@Immutable
@Singleton
class ManageCommand
@Inject
constructor(
    messagingManager: MessagingManager,
    privateMessageFactory: PrivateMessageFactory,
    clock: Clock,
    private val databaseComponent: DatabaseComponent,
    private val contactManager: ContactManager,
    private val forumSharingManager: ForumSharingManager,
    private val permissionDao: PermissionDao,
    private val variableDao: VariableDao
) : BotCommand(messagingManager, privateMessageFactory, clock) {

    @DewCommand("setvar", privileged = true)
    fun setvar(args: String, groupId: GroupId, contactId: ContactId) {
        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.MODIFY_VARIABLE)
        if(!authed) throw DewPermissionException()
        var argsplit = args.split(' ', ignoreCase = false, limit = 2)
        if(argsplit.isEmpty()) throw DewArgumentException("Missing positional parameter")
        if(argsplit.size==1){
            variableDao.set(argsplit[0], null)
        } else {
            variableDao.set(argsplit[0], argsplit[1])
        }
        sendMessage(groupId, "Variable set")
    }

    @DewCommand("getvar", privileged = true)
    fun getvar(arg: String, groupId: GroupId, contactId: ContactId) {
        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.MODIFY_VARIABLE)
        if(!authed) throw DewPermissionException()
        val result = variableDao.get(arg)
        sendMessage(groupId, "$arg = ${result?:"[NULL]"}")
    }

    @DewCommand("debug", privileged = true)
    fun debug(arg: String, groupId: GroupId, contactId: ContactId) {
        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.ADMIN)
        if(!authed) throw DewPermissionException()
        setProperty(DewConstants.DEW_DEBUG_GROUP_KEY, arg)
        sendMessage(groupId, "Debug group set to $arg")
    }



    @DewCommand("setgroupvisibility", privileged = true, shortHelp = "groupId INVISIBLE/VISIBLE/SHARED")
    fun setgroupvisibility(args: String, groupId: GroupId, contactId: ContactId) {
        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.ADMIN)
        if(!authed) throw DewPermissionException()
        var argsplit = args.split(' ', ignoreCase = false, limit = 3)
        val targetContact = ContactId(argsplit[0].toInt())
        val targetGroup = argsplit[1].base32Decode().toGroupId()
        val visibility = Visibility.valueOf(argsplit[2].uppercase())
        val contacts = forumSharingManager.getSharedWith(targetGroup)
        databaseComponent.transaction<DbException>(false) {
            for(contact in contacts) {
                databaseComponent.setGroupVisibility(it, contact.id, targetGroup, visibility)
            }
        }

    }

    @DewCommand("setvisibility", privileged = true, shortHelp = "contactId groupId INVISIBLE/VISIBLE/SHARED")
    fun setvisibility(args: String, groupId: GroupId, contactId: ContactId) {
        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.ADMIN)
        if(!authed) throw DewPermissionException()
        var argsplit = args.split(' ', ignoreCase = false, limit = 3)
        val targetContact = ContactId(argsplit[0].toInt())
        val targetGroup = argsplit[1].base32Decode().toGroupId()
        val visibility = Visibility.valueOf(argsplit[2].uppercase())
        databaseComponent.transaction<DbException>(false) {
            databaseComponent.setGroupVisibility(it, targetContact, targetGroup, visibility)
        }
    }

    @DewCommand("pendingcontact", privileged = true, shortHelp = "Add contact without web interface")
    fun pendingcontact(arg: String, groupId: GroupId, contactId: ContactId) {
        val authorId = contactManager.getContact(contactId).author.id
        val permissions = permissionDao.get(authorId)
        var authed = permissions.has(PermissionFlag.ADD_PENDING_CONTACT)
        if(!authed) throw DewPermissionException()

        try {
            contactManager.addPendingContact(arg, arg.hashCode().toString())
            sendMessage(groupId, "Pending contact added")
        } catch (e: PendingContactExistsException) {
            contactManager.removePendingContact(e.pendingContact.id)
            sendMessage(groupId, "Pending contact removed")
        }catch (e: Exception) {
            sendMessage(groupId, e.toString())
        }
    }
}
