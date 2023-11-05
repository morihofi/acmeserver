package de.morihofi.acmeserver.database.objects;

import javax.persistence.*;
import java.sql.Timestamp;

@Entity
@Table(name = "orderidentifiers")
public class ACMEOrderIdentifier {

    @Id
    @Column(name = "challengeId")
    private String challengeId;

    @Column(name = "verified")
    private boolean verified;

    @Column(name = "verifiedTime")
    private Timestamp verifiedTime;

    public String getChallengeId() {
        return challengeId;
    }

    public void setChallengeId(String challengeId) {
        this.challengeId = challengeId;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public Timestamp getVerifiedTime() {
        return verifiedTime;
    }

    public void setVerifiedTime(Timestamp verifiedTime) {
        this.verifiedTime = verifiedTime;
    }

}
