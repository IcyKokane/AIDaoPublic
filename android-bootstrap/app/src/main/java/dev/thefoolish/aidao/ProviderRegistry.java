package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central provider registry for AIDao planning/model providers.
 *
 * The deterministic local planner is always present and remains the default.
 * Remote providers may be registered as optional enhancements, but selection is
 * gated by ProviderEconomicsPolicy so core AIDao operation cannot silently
 * acquire an AIDao-funded inference/service dependency.
 */
public final class ProviderRegistry {
    private final List<ModelProvider> providers = new ArrayList<>();
    private String selectedId;
    private ProviderEconomicsPolicy.Decision lastSelectionDecision;

    public ProviderRegistry() {
        ModelProvider local = ProviderEconomicsPolicy.defaultProvider();
        if (!ProviderEconomicsPolicy.eligibleAsRequiredCoreProvider(local)) {
            throw new IllegalStateException("Default provider must remain local/zero-cost.");
        }
        providers.add(local);
        selectedId = local.id();
        lastSelectionDecision = ProviderEconomicsPolicy.evaluate(local, false, false);
    }

    public List<ModelProvider> all() {
        return Collections.unmodifiableList(providers);
    }

    public ModelProvider selected() {
        for (ModelProvider provider : providers) {
            if (provider.id().equals(selectedId)) return provider;
        }
        // Registry corruption or a future migration must fail back to the first
        // required-core provider rather than leaving AIDao without planning.
        for (ModelProvider provider : providers) {
            if (ProviderEconomicsPolicy.eligibleAsRequiredCoreProvider(provider)) {
                selectedId = provider.id();
                lastSelectionDecision = ProviderEconomicsPolicy.evaluate(provider, false, false);
                return provider;
            }
        }
        throw new IllegalStateException("No zero-cost core provider is registered.");
    }

    /**
     * Backward-compatible selection API. It intentionally cannot opt into a
     * remote provider because no explicit BYOK/user-funding signal is present.
     */
    public boolean select(String id) {
        return select(id, false, false);
    }

    /**
     * Select a provider under the economic/runtime policy.
     *
     * @param id provider id
     * @param explicitlySelected true only after a visible user opt-in
     * @param userSuppliedCredential true only when any required account/key is supplied by the user
     */
    public boolean select(String id, boolean explicitlySelected, boolean userSuppliedCredential) {
        ModelProvider provider = find(id);
        if (provider == null) {
            lastSelectionDecision = null;
            return false;
        }

        ProviderEconomicsPolicy.Decision decision = ProviderEconomicsPolicy.evaluate(
                provider,
                explicitlySelected,
                userSuppliedCredential);
        lastSelectionDecision = decision;
        if (!decision.allowed) return false;

        selectedId = provider.id();
        return true;
    }

    /** Last policy result, suitable for a visible configuration/error message. */
    public ProviderEconomicsPolicy.Decision lastSelectionDecision() {
        return lastSelectionDecision;
    }

    public void register(ModelProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider == null");
        if (provider.id() == null || provider.id().trim().isEmpty()) {
            throw new IllegalArgumentException("Provider id is required.");
        }
        for (ModelProvider existing : providers) {
            if (existing.id().equals(provider.id())) {
                throw new IllegalArgumentException("Duplicate provider id: " + provider.id());
            }
        }
        providers.add(provider);
    }

    private ModelProvider find(String id) {
        if (id == null) return null;
        for (ModelProvider provider : providers) {
            if (provider.id().equals(id)) return provider;
        }
        return null;
    }
}
