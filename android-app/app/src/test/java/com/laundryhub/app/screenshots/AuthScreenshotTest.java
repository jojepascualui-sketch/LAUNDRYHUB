package com.laundryhub.app.screenshots;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.core.app.ActivityScenario;

import com.laundryhub.app.LaundryHubApp;
import com.laundryhub.app.data.session.Session;
import com.laundryhub.app.data.session.SessionStore;
import com.laundryhub.app.ui.auth.SignInActivity;
import com.laundryhub.app.ui.auth.SignUpActivity;
import com.laundryhub.app.ui.widget.SegmentedControl;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.function.Consumer;

/**
 * Renders 00a, 00b and 00c at the design size (390×844 dp at 2×, like docs/design/screens) so
 * they can be compared with the PNGs when no emulator is available. Skipped unless run with
 * {@code ./gradlew testDebugUnitTest -Pscreenshots}; files go to build/screenshots.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, application = AuthScreenshotTest.TestApp.class, qualifiers = "w390dp-h844dp-port-xhdpi")
public class AuthScreenshotTest {

    /** The real app with an in-memory session store (Robolectric has no Android Keystore). */
    public static class TestApp extends LaundryHubApp {
        @NonNull
        @Override
        protected SessionStore createSessionStore() {
            return new SessionStore() {
                @Override
                public void save(@NonNull Session session) {
                }

                @Override
                public Session load() {
                    return null;
                }

                @Override
                public void clear() {
                }
            };
        }
    }

    @Before
    public void onlyWhenRequested() {
        Assume.assumeTrue("Run with -Pscreenshots", Boolean.getBoolean("screenshots"));
    }

    @Test
    public void signInCustomer() throws IOException {
        capture(SignInActivity.class, "00a-sign-in", a -> { });
    }

    @Test
    public void signInAdmin() throws IOException {
        capture(SignInActivity.class, "00c-admin-sign-in",
                a -> tab(a, 2));
    }

    @Test
    public void signInRider() throws IOException {
        capture(SignInActivity.class, "00a-sign-in-rider", a -> tab(a, 1));
    }

    @Test
    public void signUp() throws IOException {
        capture(SignUpActivity.class, "00b-sign-up", a -> { });
    }

    private static void tab(Activity activity, int index) {
        SegmentedControl tabs = activity.findViewById(com.laundryhub.app.R.id.role_tabs);
        tabs.getSegmentViews().get(index).performClick();
    }

    private static <A extends Activity> void capture(Class<A> screen, String name, Consumer<A> setUp)
            throws IOException {
        try (ActivityScenario<A> scenario = ActivityScenario.launch(screen)) {
            scenario.onActivity(activity -> {
                setUp.accept(activity);
                ShadowLooper.idleMainLooper();
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
