package net.enfau.berry.commands

import net.enfau.berry.DewConstants
import net.enfau.berry.utils.WithLogger
import net.enfau.berry.utils.base32Decode
import net.enfau.berry.utils.toGroupId
import org.briarproject.bramble.api.event.Event
import org.briarproject.bramble.api.event.EventListener
import org.briarproject.bramble.api.sync.GroupId
import org.briarproject.bramble.api.system.Clock
import org.briarproject.briar.api.messaging.MessagingManager
import org.briarproject.briar.api.messaging.PrivateMessageFactory
import org.briarproject.briar.api.messaging.PrivateMessageHeader
import org.briarproject.briar.api.messaging.event.PrivateMessageReceivedEvent
import java.lang.System.getProperty
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

abstract class BotCommand
constructor(
    protected val messagingManager: MessagingManager,
    protected val privateMessageFactory: PrivateMessageFactory,
    protected val clock: Clock
) : EventListener, WithLogger {
    public val DEBUG_KEY = "net.enfau.dewberry/COMMAND_DEBUG"

    val logger = logger()

    enum class CommandScope {
        Message,
        PrivateMessage,
        GroupMessage,
        ForumPost,
    }

    fun sendMessage(groupId: GroupId, text: String) {
        val msg = privateMessageFactory.createLegacyPrivateMessage(groupId, clock.currentTimeMillis(), text)
        messagingManager.addLocalMessage(msg)
    }

    final override fun eventOccurred(e: Event) {
        when (e) {
            is PrivateMessageReceivedEvent -> {
                val h = e.messageHeader
                var message: String? = null
                when (h) {
                    is PrivateMessageHeader -> {
                        val h: PrivateMessageHeader = h
                        message = messagingManager.getMessageText(h.id)
                    }

                    else -> {}
                }
                if (message == null) return

                val commandPrefixed = message.split(' ')[0]
                val command = commandPrefixed.trimStart('/')
                if (command.isEmpty()) return
                var method: Method? = null
                when (h) {
                    is PrivateMessageHeader -> method = getMethod(command, DewCommandScope.PrivateMessage)
                    else -> {}
                }
                if (method == null) return
                val types = method.parameterTypes
                val args = Array<Any?>(types.size) { null }
                setParameter(args, types, e)
                setParameter(args, types, e.contactId)
                setParameter(args, types, h)
                setParameter(args, types, h.id)
                setParameter(args, types, h.groupId)

                when (h) {
                    is PrivateMessageHeader -> {
                        val h: PrivateMessageHeader = h
                        setParameter(args, types, message.substring(commandPrefixed.length).trimStart())
                    }

                    else -> {}
                }
                try {
                    method.invoke(this, *args)
                } catch (e: InvocationTargetException) {
                    if (getProperty(DewConstants.DEW_DEBUG_KEY).isNotEmpty()) {
                        logger.debug("", e)
                        sendMessage(h.groupId, e.targetException.toString())
                    } else {
                        sendMessage(h.groupId, "An error occurred while running this command")
                    }
                }catch (e: DewPermissionException) {
                    sendMessage(h.groupId, "You don't have permission to do this")
                }catch (e: DewArgumentException) {
                    val msg = e.message?:"Invalid argument"
                    sendMessage(h.groupId, msg)
                } catch (e: Exception) {
                    logger.error("Error occurred while running command: $message", e)
                    if(getProperty(DewConstants.DEW_DEBUG_GROUP_KEY).isNotEmpty()) {
                        val debugGroup = DewConstants.DEW_DEBUG_GROUP_KEY.base32Decode().toGroupId()
                        if(h.groupId==debugGroup)
                            sendMessage(h.groupId , e.toString())
                        else
                            sendMessage(h.groupId , "An error occurred while running this command")
                    } else {
                        sendMessage(h.groupId , "An error occurred while running this command")
                    }

                }
            }
        }
    }


    private fun setParameter(paramArray: Array<Any?>, paramTypes: Array<Class<*>>, value: Any) {
        var index = paramTypes.indexOf(value::class.java)
        if (index == -1) {
            for (i in paramTypes.indices) {
                val type = paramTypes[i]
                if (value::class.java.isAssignableFrom(type)) {
                    index = i
                }
            }
        }
        if (index == -1) return
        paramArray[index] = value
    }

    private fun getMethod(command: String, scope: DewCommandScope = DewCommandScope.Message): Method? {
        val methods = this::class.java.methods
        var annotation: Class<out Annotation> = DewCommand::class.java
        for (method in methods) {
            if (method.isAnnotationPresent(annotation)) {
                val annotation = method.getAnnotation(DewCommand::class.java)
                if (annotation.name == command && annotation.scope == scope) {
                    return method
                }
                if (annotation.name == command && annotation.scope == DewCommandScope.Message) {
                    return method
                }
            }
        }
        return null
    }
}
