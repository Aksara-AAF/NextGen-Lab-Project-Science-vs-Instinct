package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "friendships",
       uniqueConstraints = @UniqueConstraint(columnNames = {"requester_id", "addressee_id"}))
public class Friendship {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requester_id", nullable = false)
    private Long requesterId;

    @Column(name = "addressee_id", nullable = false)
    private Long addresseeId;

    @Column(nullable = false)
    private String status = "PENDING";

    public Friendship() {}

    public Long   getId()          { return id; }
    public Long   getRequesterId() { return requesterId; }
    public Long   getAddresseeId() { return addresseeId; }
    public String getStatus()      { return status; }

    public void setRequesterId(Long v) { this.requesterId = v; }
    public void setAddresseeId(Long v) { this.addresseeId = v; }
    public void setStatus(String v)    { this.status = v; }
}
