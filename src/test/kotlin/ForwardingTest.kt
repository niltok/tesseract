import goldimax.tesseract.Forward
import org.jsoup.nodes.Element
import org.junit.Assert
import org.junit.Test

class ForwardingTest {

    @Test
    fun richMessageExtraction() {
        Assert.assertEquals(
            Forward.extractRichMessage(
                "<?xml version='1.0' encoding='UTF-8' standalone='yes' ?><msg serviceID=\"35\" templateID=\"1\" action=\"viewMultiMsg\" brief=\"[聊天记录]\" m_resid=\"j6LEz0SgxAux5QU7kXcANoty2CbOwMJuUW73UimRQ+r5vseJiz4OrChx+Jh1p95r\" m_fileName=\"6821792081676910470\" tSum=\"4\" sourceMsgId=\"0\" url=\"\" flag=\"3\" adverSign=\"0\" multiMsgFlag=\"0\"><item layout=\"1\" advertiser_id=\"0\" aid=\"0\"><title size=\"34\" maxLines=\"2\" lineSpace=\"12\">群聊的聊天记录</title><title size=\"26\" color=\"#777777\" maxLines=\"4\" lineSpace=\"12\">Alice:  去年我们前半段一直前五</title><title size=\"26\" color=\"#777777\" maxLines=\"4\" lineSpace=\"12\">Bob:  然后我们一直头铁一道题</title><title size=\"26\" color=\"#777777\" maxLines=\"4\" lineSpace=\"12\">Charles:  头铁出来了</title><title size=\"26\" color=\"#777777\" maxLines=\"4\" lineSpace=\"12\">Donald:  后来才知道那是防ak的题</title><hr hidden=\"false\" style=\"0\" /><summary size=\"26\" color=\"#777777\">查看4条转发消息</summary></item><source name=\"聊天记录\" icon=\"\" action=\"\" appid=\"-1\" /></msg>"
            ).map(Element::text).toString(),
            "[群聊的聊天记录, Alice:  去年我们前半段一直前五, Bob:  然后我们一直头铁一道题, Charles:  头铁出来了, Donald:  后来才知道那是防ak的题]"
        )
    }
}
