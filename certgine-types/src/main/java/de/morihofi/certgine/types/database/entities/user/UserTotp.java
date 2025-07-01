package de.morihofi.certgine.types.database.entities.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Authenticator application secret for a user.
 */
@Entity
@Table(name = "user_totp")
@Data
@NoArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UserTotp {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private Users user;

    private String nickname;

    @Column(nullable = false)
    private String secret;
}
