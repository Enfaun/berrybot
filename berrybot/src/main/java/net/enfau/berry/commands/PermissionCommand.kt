package net.enfau.berry.commands

import net.enfau.berry.dao.ContactDao
import net.enfau.berry.dao.PermissionDao
import net.enfau.berry.dao.PermissionFlag
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.bramble.api.contact.ContactManager

@Immutable
@Singleton
class PermissionCommand
@Inject
constructor(
    messagingManager: MessagingManager,
    privateMessageFactory: PrivateMessageFactory,
    clock: Clock,
    private val contactManager: ContactManager,
    private val contactDao: ContactDao,
    private val permissionDao: PermissionDao
) : BotCommand(messagingManager, privateMessageFactory, clock) {

    @DewCommand("setperm", privileged = true, longHelp = "look for id with finduser")
    fun setPerm(args: String, groupId: GroupId, contactId: ContactId) {
        val author = contactManager.getContact(contactId).author
        if(!permissionDao.get(author.id).has(PermissionFlag.MANAGE_PERMISSIONS)) return
        val argsplit = args.split(' ')
        if(argsplit.size!=3) throw DewArgumentException("need at least 3 arguments")
        val uid = argsplit[0]
        var permissionFlag: PermissionFlag
        var permIntFlag = argsplit[1].toIntOrNull()
        if(permIntFlag==null) {
            permissionFlag = PermissionFlag.valueOf(argsplit[1])
        }else {
            permissionFlag = PermissionFlag.getByValue(permIntFlag)!!
        }
        val isOn = argsplit[2].lowercase().matches("^[1y]".toRegex())
        val isOff = argsplit[2].lowercase().matches("^[0n]".toRegex())
        var perm = permissionDao.get(uid)
        if(isOn) perm.on(permissionFlag)
        if(isOff) perm.off(permissionFlag)
        val isSet = permissionDao.set(uid, perm)
        if(isSet) {
            sendMessage(groupId, "Flag ${permissionFlag.name} set to ${perm.has(permissionFlag)}")
        } else {
            sendMessage(groupId, "Failed to set flag")
        }

    }

    @DewCommand("getperm", privileged = true)
    fun getPerm(uid: String, groupId: GroupId, contactId: ContactId) {
        val author = contactManager.getContact(contactId).author
        if(!permissionDao.get(author.id).has(PermissionFlag.MANAGE_PERMISSIONS)) return
        val contact = contactDao.get(uid)
        val perm = permissionDao.get(uid)
        var msg = "Flag of ${contact?.name}:"
        for(v in PermissionFlag.values()){
            if(v==PermissionFlag.NONE) continue
            val key = v.toString()
            val status = if(perm.has(v)) "1" else "0"
            msg += "\n$status $key(${v.value})"
        }
        sendMessage(groupId, msg)
    }

}
