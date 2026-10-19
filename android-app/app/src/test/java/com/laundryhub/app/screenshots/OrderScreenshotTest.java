package com.laundryhub.app.screenshots;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.View;

import androidx.lifecycle.ViewModelProvider;
import androidx.test.core.app.ActivityScenario;

import com.laundryhub.app.model.Location;
import com.laundryhub.app.model.OrderDraft;
import com.laundryhub.app.model.PaymentOption;
import com.laundryhub.app.model.ServiceType;
import com.laundryhub.app.testing.FakeOrderRepository;
import com.laundryhub.app.ui.home.HomeActivity;
import com.laundryhub.app.ui.order.OrderWizardActivity;
import com.laundryhub.app.ui.order.OrderWizardViewModel;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLooper;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Renders 01, 02, 03, 04b, 05 and 06 at the design size with the same state as the PNGs, by
 * driving the real wizard ViewModel. Run with {@code ./gradlew testDebugUnitTest -Pscreenshots}.
 */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = 35, application = ScreenshotApp.class, qualifiers = "w390dp-h844dp-port-xhdpi")
public class OrderScreenshotTest {

    @Before
    public void onlyWhenRequested() {
        Assume.assumeTrue("Run with -Pscreenshots", Boolean.getBoolean("screenshots"));
    }

    @Test
    public void home() throws IOException {
        capture(HomeActivity.class, "01-home", a -> { });
    }

    @Test
    public void chooseService() throws IOException {
        wizard("02-choose-service", vm -> vm.selectService(ServiceType.PICKUP_DELIVERY));
    }

    @Test
    public void weightAndSoap() throws IOException {
        wizard("03-weight-and-soap", vm -> {
            vm.selectService(ServiceType.PICKUP_DELIVERY);
            vm.next();
            vm.increaseKg();
            vm.increaseKg();
            vm.selectSoap(FakeOrderRepository.HYPOALLERGENIC);
        });
    }

    @Test
    public void dropOffWalkIn() throws IOException {
        wizard("04b-drop-off-walk-in", vm -> {
            vm.selectService(ServiceType.WALK_IN);
            vm.next();
            vm.next();
            vm.selectSlot(vm.getSlots().getValue().get(2));
        });
    }

    @Test
    public void pinLocationDelivery() throws IOException {
        // The prototype has no geocoding: hold the pin check so Address details stays empty, as in the PNG.
        wizard("04a-pin-location-delivery", vm -> {
            FakeOrderRepository repo = (FakeOrderRepository) com.laundryhub.app.LaundryHubApp
                    .container(org.robolectric.RuntimeEnvironment.getApplication()).orderRepository();
            repo.holdPinChecks = true;
            vm.selectService(ServiceType.PICKUP_DELIVERY);
            vm.next();
            vm.increaseKg();
            vm.increaseKg();
            vm.selectSoap(FakeOrderRepository.HYPOALLERGENIC);
            vm.next();
            vm.setPin(Location.of(14.61, 121.01));
            vm.selectSlot(vm.getSlots().getValue().get(0));
        });
    }

    @Test
    public void reviewAndPay() throws IOException {
        wizard("05-review-and-pay", vm -> toReview(vm));
    }

    @Test
    public void orderPlaced() throws IOException {
        wizard("06-order-placed", vm -> {
            toReview(vm);
            vm.setGcashNumber("0917 000 1021");
            vm.next();
        });
    }

    /** Delivery, 10 kg, Hypoallergenic, Today 2–4 PM, pinned (Phase 5 map), GCash. */
    private static void toReview(OrderWizardViewModel vm) {
        vm.selectService(ServiceType.PICKUP_DELIVERY);
        vm.next();
        vm.increaseKg();
        vm.increaseKg();
        vm.selectSoap(FakeOrderRepository.HYPOALLERGENIC);
        vm.next();
        vm.setPin(Location.of(14.61, 121.01));
        vm.selectSlot(vm.getSlots().getValue().get(0));
        vm.next();
        vm.selectPayment(PaymentOption.GCASH);
    }

    private static void wizard(String name, Consumer<OrderWizardViewModel> setUp) throws IOException {
        capture(OrderWizardActivity.class, name, activity -> {
            OrderWizardViewModel vm = new ViewModelProvider((androidx.fragment.app.FragmentActivity) activity,
                    OrderWizardActivity.viewModelFactory(activity)).get(OrderWizardViewModel.class);
            setUp.accept(vm);
        });
    }

    private static <A extends Activity> void capture(Class<A> screen, String name, Consumer<A> setUp)
            throws IOException {
        try (ActivityScenario<A> scenario = ActivityScenario.launch(screen)) {
            scenario.onActivity(activity -> {
                setUp.accept(activity);
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
