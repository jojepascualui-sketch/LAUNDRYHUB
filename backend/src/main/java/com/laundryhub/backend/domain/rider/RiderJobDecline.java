package com.laundryhub.backend.domain.rider;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

import com.laundryhub.backend.domain.user.Rider;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** A rider's "Decline" of an available job; the job stays available to other riders. */
@Entity
@Table(name = "rider_job_declines")
public class RiderJobDecline {

    @EmbeddedId
    private Key id;

    @MapsId("jobId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id")
    private RiderJob job;

    @MapsId("riderId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id")
    private Rider rider;

    @Column(name = "declined_at", nullable = false)
    private LocalDateTime declinedAt;

    /** For JPA only. */
    protected RiderJobDecline() {
    }

    RiderJobDecline(RiderJob job, Rider rider) {
        this.id = new Key(job.getId(), rider.getId());
        this.job = job;
        this.rider = rider;
        this.declinedAt = LocalDateTime.now();
    }

    /** @return the declined job */
    public RiderJob getJob() {
        return job;
    }

    /** @return the rider who declined */
    public Rider getRider() {
        return rider;
    }

    /** @return when it was declined */
    public LocalDateTime getDeclinedAt() {
        return declinedAt;
    }

    /** Composite key (job, rider). */
    @Embeddable
    public static class Key implements Serializable {

        private static final long serialVersionUID = 1L;

        @Column(name = "job_id")
        private Long jobId;

        @Column(name = "rider_id")
        private Long riderId;

        /** For JPA only. */
        protected Key() {
        }

        Key(Long jobId, Long riderId) {
            this.jobId = jobId;
            this.riderId = riderId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof Key other && Objects.equals(jobId, other.jobId) && Objects.equals(riderId, other.riderId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(jobId, riderId);
        }
    }
}
