package de.morihofi.acmeserver.database.objects;

import jakarta.persistence.*;
import java.io.Serializable;
import java.sql.Timestamp;

/**
 * Represents an ACME order entity used for managing certificate orders.
 */
@Entity
@Table(name = "orders")
public class ACMEOrder implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "orderId", unique = true)
    private String orderId;

    @ManyToOne
    @JoinColumn(name = "accountId", referencedColumnName = "accountId")
    private ACMEAccount account;

    @Column(name = "created")
    private Timestamp created;

    /**
     * Get the unique identifier of the ACME order.
     * @return The order ID.
     */
    public String getOrderId() {
        return orderId;
    }

    /**
     * Set the unique identifier of the ACME order.
     * @param orderId The order ID to set.
     */
    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    /**
     * Get the ACME account associated with this order.
     * @return The ACME account.
     */
    public ACMEAccount getAccount() {
        return account;
    }

    /**
     * Set the ACME account associated with this order.
     * @param account The ACME account to set.
     */
    public void setAccount(ACMEAccount account) {
        this.account = account;
    }

    /**
     * Get the timestamp when the ACME order was created.
     * @return The creation timestamp.
     */
    public Timestamp getCreated() {
        return created;
    }

    /**
     * Set the timestamp when the ACME order was created.
     * @param created The creation timestamp to set.
     */
    public void setCreated(Timestamp created) {
        this.created = created;
    }
}
