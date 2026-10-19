package com.laundryhub.app.screenshots;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.test.core.app.ActivityScenario;

import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.testing.SampleTracking;
import com.laundryhub.app.ui.track.TrackOrderActivity;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

/** Renders 07-track-order in the prototype's state (step "Out for delivery"). Run with -Pscreenshots. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, application = ScreenshotApp.class, qualifiers = "w390dp-h844dp-port-xhdpi")
public class TrackScreenshotTest {

    @Before
    public void onlyWhenRequested() {
        Assume.assumeTrue("Run with -Pscreenshots", Boolean.getBoolean("screenshots"));
    }

    @Test
    public void trackOrder() {
        FakeOrderRepository repo = (FakeOrderRepository) LaundryHubApp
                .container(RuntimeEnvironment.getApplication()).orderRepository();
        repo.tracking = SampleTracking.at(3);
        try (ActivityScenario<TrackOrderActivity> scenario = ActivityScenario.launch(
                TrackOrderActivity.intent(RuntimeEnvironment.getApplication(), 1))) {
            scenario.onActivity(activity -> {
                ShadowLooper.idleMainLooper(1, TimeUnit.SECONDS);
                View root = activity.getWindow().getDecorView();
                Bitmap bitmap = Bitmap.createBitmap(root.getWidth(), root.getHeight(), Bitmap.Config.ARGB_8888);
                root.draw(new Canvas(bitmap));
                File dir = new File(System.getProperty("screenshotDir", "build/screenshots"));
                if (!dir.isDirectory() && !dir.mkdirs()) {
                    throw new IllegalStateException("Cannot create " + dir);
                }
                try (FileOutputStream out = new FileOutputStream(new File(dir, "07-track-order.png"))) {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }
            });
        }
    }
}
