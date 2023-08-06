package net.enfau.berry.commands

import net.enfau.berry.dao.PermissionDao
import net.enfau.berry.dao.PermissionFlag
import org.briarproject.bramble.api.contact.ContactId
import org.briarproject.bramble.api.contact.ContactManager
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import javax.annotation.concurrent.Immutable
import javax.inject.Inject
import javax.inject.Singleton


@Immutable
@Singleton
class HelpCommand
@Inject
constructor(
    messagingManager: MessagingManager,
    privateMessageFactory: PrivateMessageFactory,
    clock: Clock,
    private val contactManager: ContactManager,
    private val permissionDao: PermissionDao
) : BotCommand(messagingManager, privateMessageFactory, clock), HelpRegisterer {

    private var allHelpMessage = "All commands"
    private var adminHelpMessage = ""

    private var individualHelpMessages = HashMap<String, String>()
    private var individualAdminHelpMessages = HashMap<String, String>()

    @DewCommand("help", shortHelp = "Show help")
    fun help(cmd: String, groupId: GroupId) {
        var helpMessage = allHelpMessage
        if(individualHelpMessages.containsKey(cmd))
            helpMessage = individualHelpMessages.getOrDefault(cmd, "")
        sendMessage(groupId, helpMessage)
    }

    @DewCommand("adminhelp", shortHelp = "Show help", privileged = true)
    fun adminhelp(cmd: String, groupId: GroupId, contactId: ContactId) {
        val contact = contactManager.getContact(contactId)
        val perm = permissionDao.get(contact.author.id)
        if(!perm.has(PermissionFlag.ADMIN)) return
        var helpMessage = adminHelpMessage
        if(individualAdminHelpMessages.containsKey(cmd))
            helpMessage = individualAdminHelpMessages.getOrDefault(cmd, "")
        sendMessage(groupId, helpMessage)

    }

    override fun register(cls: Class<*>) {
        val methods = cls.methods
        for(method in methods) {
            if(method.isAnnotationPresent(DewCommand::class.java)){
                val annotation = method.getAnnotation(DewCommand::class.java)
                if(!annotation.privileged){
                    logger.debug("Adding command help /${annotation.name}")
                    allHelpMessage += "\n/${annotation.name}\n    ${annotation.shortHelp}"
                    individualHelpMessages[annotation.name] = "/${annotation.name} - ${annotation.shortHelp}\n${annotation.longHelp}".trim()
                }else {
                    logger.debug("Adding admin command help /${annotation.name}")
                    adminHelpMessage += "\n/${annotation.name}\n    ${annotation.shortHelp}"
                    individualAdminHelpMessages[annotation.name] = "/${annotation.name} - ${annotation.shortHelp}\n${annotation.longHelp}".trim()

                }
            }
        }
        allHelpMessage = allHelpMessage.trim()
    }
}
