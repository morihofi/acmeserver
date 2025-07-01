package de.morihofi.certgine.types.database.entities.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * WebAuthn credential registered by a user.
 */
@Entity
@Table(name = "user_webauthn")
@Data
@NoArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UserWebAuthnKey {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    private String nickname;

    @Column(nullable = false)
    private String credentialId;

    @Column(nullable = false, length = 2048)
    private String publicKey;
}
