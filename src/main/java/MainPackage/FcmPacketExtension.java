package MainPackage;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;

public class FcmPacketExtension implements ExtensionElement {

    private final String json;

    public FcmPacketExtension(String json) {
        this.json = json;
    }

    public String getJson() {
        return json;
    }

    @Override
    public String getNamespace() {
        return Utils.FCM_NAMESPACE;
    }

    @Override
    public String getElementName() {
        return Utils.FCM_ELEMENT_NAME;
    }

    @Override
    public CharSequence toXML(String enclosingNamespace) {
        return String.format("<%s xmlns=\"%s\">%s</%s>", getElementName(),
                getNamespace(), json, Utils.FCM_ELEMENT_NAME);
    }

    public Stanza toPacket() {
        final Message message = new Message();
        message.addExtension(this);
        return message;
    }
}
