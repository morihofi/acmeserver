package de.morihofi.certgine.types.database.entities.user;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

/**
 * Groups for organizing users. Groups may have administrative privileges.
 */
@Entity
@Table(name = "user_group")
@Data
@NoArgsConstructor
@SuppressFBWarnings({"EI_EXPOSE_REP", "EI_EXPOSE_REP2"})
public class UserGroup {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    private String name;

    private boolean admin;

    @ManyToMany(mappedBy = "groups")
    private Set<Users> users;
}
