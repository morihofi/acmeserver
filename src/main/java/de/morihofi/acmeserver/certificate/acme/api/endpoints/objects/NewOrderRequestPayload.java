// NewOrderRequestPayload.java

// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;
import java.util.List;

public class NewOrderRequestPayload {
    private List<Identifier> identifiers;

    public List<Identifier> getIdentifiers() { return identifiers; }
    public void setIdentifiers(List<Identifier> value) { this.identifiers = value; }
}
