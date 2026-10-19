package com.laundryhub.app.model.rider;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.testing.FakeRiderRepository;

import org.junit.Test;

import java.math.BigDecimal;
import java.util.List;

public class RiderJobTest {

    private static final Location CUSTOMER = Location.of(14.61, 121.0);

    private static RiderJob job(JobType type, PaymentOption pay, int done) {
        List<JobStep> steps = FakeRiderRepository.stepsFor(type, pay);
        return new RiderJob(1, "LH-0001", type, RiderJob.Status.IN_PROGRESS, null, "Ana Reyes", "AR", null,
                "Brgy. Poblacion", CUSTOMER, FakeRiderRepository.SHOP, "Today, 4–6 PM", new BigDecimal("12"),
                new BigDecimal("516.00"), pay, steps, done, false);
    }

    @Test
    public void pickupGoesToTheCustomerThenTheShop() {
        assertEquals(CUSTOMER, job(JobType.PICKUP, PaymentOption.CASH, 0).nextStop());
        assertEquals(CUSTOMER, job(JobType.PICKUP, PaymentOption.CASH, 2).nextStop());
        assertEquals(FakeRiderRepository.SHOP, job(JobType.PICKUP, PaymentOption.CASH, 3).nextStop());
    }

    @Test
    public void returnDeliveryGoesToTheShopThenTheCustomer() {
        assertEquals(FakeRiderRepository.SHOP, job(JobType.RETURN_DELIVERY, PaymentOption.CASH, 0).nextStop());
        assertEquals(CUSTOMER, job(JobType.RETURN_DELIVERY, PaymentOption.CASH, 1).nextStop());
    }

    @Test
    public void buttonLabelFollowsTheCurrentStep() {
        assertEquals("Start trip", job(JobType.PICKUP, PaymentOption.CASH, 0).currentActionLabel());
        assertEquals("I’ve arrived", job(JobType.PICKUP, PaymentOption.CASH, 1).currentActionLabel());
        assertEquals("Cash received", job(JobType.RETURN_DELIVERY, PaymentOption.CASH, 3).currentActionLabel());
        assertEquals("Mark as delivered", job(JobType.RETURN_DELIVERY, PaymentOption.GCASH, 3).currentActionLabel());
    }

    @Test
    public void allStepsDoneMeansCompleted() {
        RiderJob done = job(JobType.PICKUP, PaymentOption.CASH, 4);
        assertTrue(done.isCompleted());
        assertNull(done.currentActionLabel());
        assertNull(done.nextStop());
        assertFalse(job(JobType.PICKUP, PaymentOption.CASH, 0).isStarted());
        assertTrue(job(JobType.PICKUP, PaymentOption.CASH, 1).isStarted());
    }

    @Test(expected = IllegalArgumentException.class)
    public void stepIndexMustBeInRange() {
        job(JobType.PICKUP, PaymentOption.CASH, 5);
    }

    @Test(expected = IllegalArgumentException.class)
    public void stepsNeedTexts() {
        new JobStep(" ", "Start trip");
    }
}
