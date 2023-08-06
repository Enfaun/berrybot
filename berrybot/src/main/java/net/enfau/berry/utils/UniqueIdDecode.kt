package net.enfau.berry.utils

import io.matthewnelson.encoding.base32.Base32
import io.matthewnelson.encoding.base32.Base32Crockford
import io.matthewnelson.encoding.core.Decoder.Companion.decodeToByteArray
import io.matthewnelson.encoding.core.Encoder.Companion.encodeToString
import org.briarproject.bramble.api.UniqueId
import org.briarproject.bramble.api.identity.AuthorId
import java.util.*
import me.ccampo.cockford32.Cockford32
import org.briarproject.bramble.api.sync.GroupId

val base32Crockford = Base32Crockford {
    isLenient = true
    encodeToLowercase = false
}

fun ByteArray.toBase64(): String =
    String(Base64.getEncoder().encode(this))

fun ByteArray.toBase32(): String =
    this.encodeToString(base32Crockford)

fun ByteArray.toAuthorId(): AuthorId =
    AuthorId(this)

fun ByteArray.toGroupId(): GroupId =
    GroupId(this)

fun ByteArray.toUniqueId(): UniqueId =
    UniqueId(this)

fun AuthorId.toBase64(): String =
    String(Base64.getEncoder().encode(this.bytes))

fun UniqueId.toBase32(): String =
    this.bytes.encodeToString(base32Crockford)
fun UniqueId.toBase64(): String =
    String(Base64.getEncoder().encode(this.bytes))


fun String.base32Decode(): ByteArray =
    this.decodeToByteArray(base32Crockford)

fun String.base32ToAuthorId(): AuthorId =
    AuthorId(this.decodeToByteArray(base32Crockford))

fun String.decodeBase64ToAuthorId(): AuthorId =
    AuthorId(Base64.getDecoder().decode(this))
