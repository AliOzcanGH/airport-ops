package com.aliozcan.airportops.iam_service.domain.model;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "member_roles", schema = "iam")
public class MemberRoleEntity {

    @EmbeddedId
    private MemberRoleId id;

    protected MemberRoleEntity() {
    }

    private MemberRoleEntity(MemberRoleId id) {
        this.id = id;
    }

    public static MemberRoleEntity assign(UUID memberId, UUID roleId) {
        return new MemberRoleEntity(new MemberRoleId(memberId, roleId));
    }

    public MemberRoleId getId() {
        return id;
    }
}
