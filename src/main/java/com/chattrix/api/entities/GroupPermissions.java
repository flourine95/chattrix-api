package com.chattrix.api.entities;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "group_permissions")
public class GroupPermissions {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false, unique = true)
    private Conversation conversation;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "send_messages", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel sendMessages = PermissionLevel.ALL;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "add_members", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel addMembers = PermissionLevel.ADMIN_ONLY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "remove_members", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel removeMembers = PermissionLevel.ADMIN_ONLY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "edit_group_info", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel editGroupInfo = PermissionLevel.ADMIN_ONLY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "pin_messages", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel pinMessages = PermissionLevel.ADMIN_ONLY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "delete_messages", nullable = false, length = 20)
    @Builder.Default
    private DeletePermissionLevel deleteMessages = DeletePermissionLevel.ADMIN_ONLY;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "create_polls", nullable = false, length = 20)
    @Builder.Default
    private PermissionLevel createPolls = PermissionLevel.ALL;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
    
    public enum PermissionLevel {
        ALL,           // Everyone can do this
        ADMIN_ONLY     // Only admins can do this
    }
    
    public enum DeletePermissionLevel {
        OWNER,         // Only message owner can delete
        ADMIN_ONLY,    // Only admins can delete any message
        ALL            // Anyone can delete any message
    }
}
