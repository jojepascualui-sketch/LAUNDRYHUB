package com.laundryhub.app.screenshots;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.test.core.app.ActivityScenario;

import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.testing.FakeFeedbackRepository;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.testing.SampleTracking;
import com.laundryhub.app.ui.feedback.ChatActivity;
import com.laundryhub.app.ui.track.TrackOrderActivity;

import android.content.Intent;

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

/** Renders the feedback chat and the 07 rating card (no design; for review). Run with -Pscreenshots. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, application = ScreenshotApp.class, qualifiers = "w390dp-h844dp-port-xhdpi")
public class FeedbackScreenshotTest {

    @Before
    public void onlyWhenRequested() {
        Assume.assumeTrue("Run with -Pscreenshots", Boolean.getBoolean("screenshots"));
    }

    @Test
    public void chat() {
        FakeFeedbackRepository feedback = (FakeFeedbackRepository) LaundryHubApp
                .container(RuntimeEnvironment.getApplication()).feedbackRepository();
        feedback.shopReplies("Hi Ana! How can we help?");
        feedback.send("My order LH-0001 smells great, thank you. Can I add 2 kg next time at pickup?",
                new com.laundryhub.app.data.ApiCallback<>() {
                    @Override
                    public void onSuccess(@androidx.annotation.NonNull com.laundryhub.app.model.feedback.ChatMessage v) {
                    }

                    @Override
                    public void onError(@androidx.annotation.NonNull com.laundryhub.app.data.ApiException e) {
                    }
                });
        feedback.shopReplies("Yes, just tell the rider. The scale at the shop sets the final weight.");
        capture(ChatActivity.intent(RuntimeEnvironment.getApplication()), "X1-feedback-chat");
    }

    @Test
    public void emptyChat() {
        capture(ChatActivity.intent(RuntimeEnvironment.getApplication()), "X2-feedback-chat-empty");
    }

    @Test
    public void ratingCard() {
        FakeOrderRepository repo = (FakeOrderRepository) LaundryHubApp
                .container(RuntimeEnvironment.getApplication()).orderRepository();
        repo.tracking = SampleTracking.at(5);
        capture(TrackOrderActivity.intent(RuntimeEnvironment.getApplication(), 1), "X3-track-completed-rating");
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
