// NewOrderRequestPayload.java

// YApi QuickType插件生成，具体参考文档:https://plugins.jetbrains.com/plugin/18847-yapi-quicktype/documentation

package de.morihofi.acmeserver.certificate.acme.api.endpoints.objects;
import java.util.List;

public class NewOrderRequestPayload {

    /**
     * List of identifiers, which the clients want to get certificates for
     */
    private List<Identifier> identifiers;

    /**
     * Retrieves the list of identifiers associated with this instance.
     * Identifiers typically represent entities such as domain names or email addresses,
     * which are relevant to the context of this object.
     *
     * @return A list of {@link Identifier} objects representing the identifiers.
     */
    public List<Identifier> getIdentifiers() {
        return identifiers;
    }

    /**
     * Sets the list of identifiers for this instance.
     * This method allows updating the identifiers, which could be domain names,
     * email addresses, or other relevant entities.
     *
     * @param value A list of {@link Identifier} objects to set as the new identifiers.
     */
    public void setIdentifiers(List<Identifier> value) {
        this.identifiers = value;
    }

}
