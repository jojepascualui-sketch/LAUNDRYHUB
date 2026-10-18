package com.laundryhub.backend.domain.rider;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.laundryhub.backend.domain.common.Check;
import com.laundryhub.backend.domain.common.Location;
import com.laundryhub.backend.domain.user.Rider;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** One GPS point sent by a rider's phone ("Rider is on the way · live GPS", screen 07). */
@Entity
@Table(name = "rider_locations")
public class RiderLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rider_id", nullable = false)
    private Rider rider;

    @Column(name = "latitude", nullable = false, precision = 9, scale = Location.COORDINATE_SCALE)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 9, scale = Location.COORDINATE_SCALE)
    private BigDecimal longitude;

    @Column(name = "accuracy_meters", precision = 7, scale = 2)
    private BigDecimal accuracyMeters;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    /** For JPA only. */
    protected RiderLocation() {
    }

    /**
     * Records a GPS point.
     *
     * @param rider          the rider
     * @param position       where the phone is (address is ignored)
     * @param accuracyMeters GPS accuracy, or {@code null}
     */
    public RiderLocation(Rider rider, Location position, BigDecimal accuracyMeters) {
        this.rider = Check.notNull(rider, "Rider");
        Check.notNull(position, "Position");
        this.latitude = position.getLatitude();
        this.longitude = position.getLongitude();
        this.accuracyMeters = accuracyMeters == null ? null : Check.nonNegative(accuracyMeters, "Accuracy");
        this.recordedAt = LocalDateTime.now();
    }

    /** @return the rider */
    public Rider getRider() {
        return rider;
    }

    /** @return the point as a location without address */
    public Location getPosition() {
        return new Location(latitude, longitude, null);
    }

    /** @return GPS accuracy in metres, or {@code null} */
    public BigDecimal getAccuracyMeters() {
        return accuracyMeters;
    }

    /** @return when the point was recorded */
    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }
}
