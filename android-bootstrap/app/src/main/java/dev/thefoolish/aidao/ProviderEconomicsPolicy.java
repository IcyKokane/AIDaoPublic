package dev.thefoolish.aidao;

/**
 * Commercial/runtime policy for model providers.
 *
 * Core AIDao operation must not create an AIDao-funded recurring service bill.
 * The default path therefore remains deterministic/local and zero-cost. Remote
 * providers are optional enhancements and may only run after explicit user
 * selection with user-supplied credentials or another user-controlled funding
 * boundary.
 */
public final class ProviderEconomicsPolicy {
    public enum FundingMode {
        LOCAL_ZERO_COST,
        USER_SUPPLIED_ZERO_COST,
        USER_FUNDED_OPTIONAL
    }

    public static final class Decision {
        public final boolean allowed;
        public final FundingMode fundingMode;
        public final String reason;

        Decision(boolean allowed, FundingMode fundingMode, String reason) {
            this.allowed = allowed;
            this.fundingMode = fundingMode;
            this.reason = reason;
        }
    }

    private ProviderEconomicsPolicy() {}

    /** The mandatory/default provider path for core planning. */
    public static ModelProvider defaultProvider() {
        return new LocalPlanningProvider();
    }

    /**
     * Classify and authorize a provider without ever silently accepting an
     * AIDao-funded remote dependency.
     *
     * @param provider provider being considered
     * @param explicitlySelected true only after a user intentionally enables it
     * @param userSuppliedCredential true only when any required key/account is supplied by the user
     */
    public static Decision evaluate(
            ModelProvider provider,
            boolean explicitlySelected,
            boolean userSuppliedCredential) {
        if (provider == null) {
            return new Decision(false, FundingMode.LOCAL_ZERO_COST, "No provider selected.");
        }

        if (!provider.isRemote()) {
            return new Decision(true, FundingMode.LOCAL_ZERO_COST,
                    "Local provider is eligible for the zero-cost core path.");
        }

        if (!explicitlySelected) {
            return new Decision(false, FundingMode.USER_FUNDED_OPTIONAL,
                    "Remote providers are optional and require explicit user selection.");
        }

        if (!userSuppliedCredential) {
            return new Decision(false, FundingMode.USER_FUNDED_OPTIONAL,
                    "Remote providers may not consume AIDao-funded credentials or service budget; use a user-supplied credential/account.");
        }

        return new Decision(true, FundingMode.USER_FUNDED_OPTIONAL,
                "Optional remote provider authorized under a user-controlled credential/funding boundary.");
    }

    /**
     * Release-gate invariant: a provider cannot be mandatory for core operation
     * when it is remote or would require AIDao-funded service spend.
     */
    public static boolean eligibleAsRequiredCoreProvider(ModelProvider provider) {
        return provider != null && !provider.isRemote();
    }
}
