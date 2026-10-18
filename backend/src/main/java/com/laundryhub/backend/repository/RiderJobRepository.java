package com.laundryhub.backend.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.laundryhub.backend.domain.order.Order;
import com.laundryhub.backend.domain.rider.JobStatus;
import com.laundryhub.backend.domain.rider.RiderJob;
import com.laundryhub.backend.domain.user.Rider;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Pickup and return-delivery jobs. */
// OOP: Abstraction — callers depend on this interface; Spring Data generates the implementation.
public interface RiderJobRepository extends JpaRepository<RiderJob, Long> {

    /**
     * R1 "Available": open jobs this rider has not declined, oldest first.
     *
     * @param rider  the rider looking at the list
     * @param status normally {@link JobStatus#AVAILABLE}
     * @return the jobs
     */
    @Query("""
            select j from RiderJob j
            where j.status = :status
              and not exists (select d from RiderJobDecline d where d.job = j and d.rider = :rider)
            order by j.availableAt asc
            """)
    List<RiderJob> findOpenFor(@Param("rider") Rider rider, @Param("status") JobStatus status);

    /**
     * R1 "My jobs": jobs assigned to the rider with the given statuses.
     *
     * @param rider    the rider
     * @param statuses e.g. ACCEPTED and IN_PROGRESS
     * @return the jobs, newest first
     */
    List<RiderJob> findByRiderAndStatusInOrderByAcceptedAtDesc(Rider rider, List<JobStatus> statuses);

    /**
     * @param order an order
     * @return the order's jobs
     */
    List<RiderJob> findByOrder(Order order);

    /**
     * @param orderId an order id
     * @return the order's jobs
     */
    List<RiderJob> findByOrderId(Long orderId);

    /**
     * @param id      job id
     * @param riderId the rider it must be assigned to
     * @return the job, if it belongs to that rider
     */
    Optional<RiderJob> findByIdAndRiderId(Long id, Long riderId);

    /**
     * R1 "Done today" counter.
     *
     * @param rider  the rider
     * @param status {@link JobStatus#COMPLETED}
     * @param since  start of today
     * @return how many jobs the rider finished since then
     */
    long countByRiderAndStatusAndCompletedAtGreaterThanEqual(Rider rider, JobStatus status, LocalDateTime since);

    /**
     * @param rider    a rider
     * @param statuses statuses to count
     * @return how many of the rider's jobs have one of them (A2 rider dropdown "active jobs")
     */
    long countByRiderAndStatusIn(Rider rider, List<JobStatus> statuses);
}
