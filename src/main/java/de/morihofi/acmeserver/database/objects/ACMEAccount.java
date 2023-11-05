package de.morihofi.acmeserver.database.objects;

import javax.persistence.*;
import java.util.List;

@Entity
@Table(name = "accounts")
public class ACMEAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "accountId")
    private String accountId;

    @Column(name = "jwt")
    private String jwt;

    @ElementCollection
    @CollectionTable(name = "account_emails", joinColumns = @JoinColumn(name = "account_id"))
    @Column(name = "email")
    private List<String> emails;

    @Column(name = "deactivated")
    private Boolean deactivated;



    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getJwt() {
        return jwt;
    }

    public void setJwt(String jwt) {
        this.jwt = jwt;
    }


    public Boolean isDeactivated() {
        return deactivated;
    }

    public void setDeactivated(Boolean deactivated) {
        this.deactivated = deactivated;
    }

    public List<String> getEmails() {
        return emails;
    }

    public void setEmails(List<String> emails) {
        this.emails = emails;
    }

    public Boolean getDeactivated() {
        return deactivated;
    }
}
