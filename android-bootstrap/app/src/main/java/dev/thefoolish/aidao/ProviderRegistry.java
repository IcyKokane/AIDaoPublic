package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Central provider registry. The offline planner is always available and remains the default.
 * Future remote providers must be registered explicitly and should require visible user setup.
 */
public final class ProviderRegistry {
    private final List<ModelProvider> providers = new ArrayList<>();
    private String selectedId;

    public ProviderRegistry() {
        ModelProvider local = new LocalPlanningProvider();
        providers.add(local);
        selectedId = local.id();
    }

    public List<ModelProvider> all() {
        return Collections.unmodifiableList(providers);
    }

    public ModelProvider selected() {
        for (ModelProvider provider : providers) {
            if (provider.id().equals(selectedId)) return provider;
        }
        return providers.get(0);
    }

    public boolean select(String id) {
        if (id == null) return false;
        for (ModelProvider provider : providers) {
            if (provider.id().equals(id)) {
                selectedId = id;
                return true;
            }
        }
        return false;
    }

    public void register(ModelProvider provider) {
        if (provider == null) throw new IllegalArgumentException("provider == null");
        for (ModelProvider existing : providers) {
            if (existing.id().equals(provider.id())) throw new IllegalArgumentException("Duplicate provider id: " + provider.id());
        }
        providers.add(provider);
    }
}
