package niltok.tesseract

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.mamoe.mirai.message.data.MessageSource

@Serializable
sealed class IMChat

@Serializable
@SerialName("IM.User")
sealed class IMUser: IMChat() {
    @Serializable
    @SerialName("IM.User.QQ")
    data class QQ(val id: Long) : IMUser()

    @Serializable
    @SerialName("IM.User.TG")
    data class TG(val id: Long) : IMUser()
}

@Serializable
@SerialName("IM.Group")
sealed class IMGroup: IMChat() {
    @Serializable
    @SerialName("IM.Group.QQ")
    data class QQ(val id: Long) : IMGroup()

    @Serializable
    @SerialName("IM.Group.TG")
    data class TG(val id: Long) : IMGroup()
}

@Serializable
sealed class IMMember {
    @Serializable
    @SerialName("IM.Member.QQ")
    data class QQ(val group: Long, val id: Long): IMMember()

    @Serializable
    @SerialName("IM.Member.TG")
    data class TG(val group: Long, val id: Long): IMMember()
}

@Serializable
sealed class Connection(var name: String, var enable: Boolean) {
    @Serializable
    @SerialName("Connection.Group")
    class GroupForward : Connection {
        val groups: MutableList<IMGroup>

        constructor(name: String, enable: Boolean, groups: MutableList<IMGroup>) : super(name, enable) {
            this.groups = groups
        }
    }

    @Serializable
    @SerialName("Connection.Single")
    class SingleForward : Connection {
        val from: IMGroup
        val to: IMGroup

        constructor(name: String, enable: Boolean, from: IMGroup, to: IMGroup) : super(name, enable) {
            this.from = from
            this.to = to
        }
    }
}

@Serializable
sealed class IMMsgRef {
    @Serializable
    @SerialName("Msg.Ref.TG")
    data class TG(val chat: Long, val user: Long?, val id: Long): IMMsgRef()

    @Serializable
    @SerialName("Msg.Ref.QQ")
    data class QQ(val source: MessageSource): IMMsgRef()
}

@Serializable
data class UniMsgMeta(
    val info: IMMsgRef,
    val reply: IMMsgRef,
    val rel: List<IMMsgRef>
)

@Serializable
sealed class UniMsgType {
    @Serializable
    @SerialName("Msg.Text")
    data class Text(val text: String): UniMsgType()

    @Serializable
    @SerialName("Msg.Image")
    data class Image(val id: String): UniMsgType()

    @Serializable
    @SerialName("Msg.Mention")
    data class Mention(val user: IMUser): UniMsgType()

    @Serializable
    @SerialName("Msg.MentionAll")
    object MentionAll : UniMsgType()
}

@Serializable
data class UniMsg(val meta: UniMsgMeta, val msg: List<UniMsgType>)