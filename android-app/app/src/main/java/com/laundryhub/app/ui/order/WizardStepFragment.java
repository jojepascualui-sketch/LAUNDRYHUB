package com.laundryhub.app.ui.order;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

/** Base of the wizard step screens: all of them share the activity's {@link OrderWizardViewModel}. */
// OOP: Abstraction — each step only renders and forwards taps; the order state lives in the ViewModel.
// OOP: Inheritance — ServiceStepFragment, LoadStepFragment, ScheduleStepFragment, ReviewStepFragment.
public abstract class WizardStepFragment extends Fragment {

    /**
     * Required empty constructor.
     *
     * @param layout the step layout
     */
    protected WizardStepFragment(int layout) {
        super(layout);
    }

    /** @return the shared ViewModel (same instance in every step) */
    @NonNull
    protected OrderWizardViewModel viewModel() {
        return new ViewModelProvider(requireActivity(), OrderWizardActivity.viewModelFactory(requireContext()))
                .get(OrderWizardViewModel.class);
    }
}
