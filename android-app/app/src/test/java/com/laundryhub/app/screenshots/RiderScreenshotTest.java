package com.laundryhub.app.screenshots;

import android.Manifest;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.test.core.app.ActivityScenario;

import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.testing.FakeRiderRepository;
import com.laundryhub.app.ui.rider.ActiveJobActivity;
import com.laundryhub.app.ui.rider.RiderHomeActivity;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowApplication;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Renders R1–R5 at the design size with the prototype's start states (source/RiderPickup,
 * RiderDelivery, RiderDone, RiderOffDuty). Run with {@code ./gradlew testDebugUnitTest -Pscreenshots}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, application = ScreenshotApp.class, qualifiers = "w390dp-h844dp-port-xhdpi")
public class RiderScreenshotTest {

    @Before
    public void onlyWhenRequested() {
        Assume.assumeTrue("Run with -Pscreenshots", Boolean.getBoolean("screenshots"));
        // A rider phone that allowed location (the design shows no permission prompt).
        ShadowApplication app = Shadow.extract(RuntimeEnvironment.getApplication());
        app.grantPermissions(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION);
    }

    private static FakeRiderRepository rider() {
        return (FakeRiderRepository) LaundryHubApp.container(RuntimeEnvironment.getApplication()).riderRepository();
    }

    @Test
    public void bookings() {
        capture(home(), "R1-rider-bookings");
    }

    @Test
    public void pickupJob() {
        rider().acceptAt(FakeRiderRepository.ANA, 1);
        capture(job(FakeRiderRepository.ANA), "R2-rider-pickup-job");
    }

    @Test
    public void deliveryCash() {
        rider().acceptAt(FakeRiderRepository.CARLO, FakeRiderRepository.CASH_STEP);
        capture(job(FakeRiderRepository.CARLO), "R3-rider-delivery-cash");
    }

    @Test
    public void jobDone() {
        rider().acceptAt(FakeRiderRepository.CARLO, FakeRiderRepository.DELIVERY_CASH_STEPS.size());
        capture(job(FakeRiderRepository.CARLO), "R4-rider-job-done");
    }

    @Test
    public void offDuty() {
        rider().onDuty = false;
        capture(home(), "R5-rider-off-duty");
    }

    private static Intent home() {
        return new Intent(RuntimeEnvironment.getApplication(), RiderHomeActivity.class);
    }

    private static Intent job(long id) {
        return ActiveJobActivity.intent(RuntimeEnvironment.getApplication(), id);
    }

    private static void capture(Intent intent, String name) {
        try (ActivityScenario<?> scenario = ActivityScenario.launch(intent)) {
            scenario.onActivity(activity -> {
                ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS);
                View root = activity.getWindow().getDecorView();
                Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
                root.draw(new Canvas(bitmap));
                File dir = new File(System.getProperty("screenshotDir", "build/screenshots"));
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IllegalStateException("Cannot create " + dir);
                }
                try (FileOutputStream out = new FileOutputStream(new File(dir, name + ".png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}
