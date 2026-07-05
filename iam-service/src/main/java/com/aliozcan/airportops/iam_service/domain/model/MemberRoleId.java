package com.aliozcan.airportops.iam_service.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MemberRoleId implements Serializable {

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Column(name = "role_id", nullable = false)
    private UUID roleId;

    protected MemberRoleId() {
    }

    public MemberRoleId(UUID memberId, UUID roleId) {
        this.memberId = memberId;
        this.roleId = roleId;
    }

    public UUID getMemberId() {
        return memberId;
    }

    public UUID getRoleId() {
        return roleId;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MemberRoleId that)) {
            return false;
        }
        return Objects.equals(memberId, that.memberId)
                && Objects.equals(roleId, that.roleId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(memberId, roleId);
    }
}
