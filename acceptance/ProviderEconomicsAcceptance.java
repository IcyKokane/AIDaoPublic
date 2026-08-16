package dev.thefoolish.aidao;

public final class ProviderEconomicsAcceptance {
    private static final class RemoteProvider implements ModelProvider {
        @Override public String id() { return "remote-test"; }
        @Override public String displayName() { return "Remote Test"; }
        @Override public boolean isRemote() { return true; }
        @Override public Result plan(Request request) {
            return new Result(ProjectPlanner.build(request.brief, request.context), displayName(), true, "test");
        }
    }

    public static void main(String[] args) throws Exception {
        ModelProvider local = ProviderEconomicsPolicy.defaultProvider();
        require(local instanceof LocalPlanningProvider, "Default provider must remain the deterministic local planner.");
        require(!local.isRemote(), "Default provider must not require remote inference.");
        require(ProviderEconomicsPolicy.eligibleAsRequiredCoreProvider(local), "Local provider must be eligible for required core operation.");

        ProviderEconomicsPolicy.Decision localDecision = ProviderEconomicsPolicy.evaluate(local, false, false);
        require(localDecision.allowed, "Local zero-cost provider must work without opt-in or credentials.");
        require(localDecision.fundingMode == ProviderEconomicsPolicy.FundingMode.LOCAL_ZERO_COST,
                "Local provider must be classified as LOCAL_ZERO_COST.");

        ModelProvider remote = new RemoteProvider();
        require(!ProviderEconomicsPolicy.eligibleAsRequiredCoreProvider(remote),
                "Remote provider must never become a required core dependency.");

        ProviderEconomicsPolicy.Decision silentRemote = ProviderEconomicsPolicy.evaluate(remote, false, true);
        require(!silentRemote.allowed, "Remote provider must not run without explicit user selection.");

        ProviderEconomicsPolicy.Decision noByok = ProviderEconomicsPolicy.evaluate(remote, true, false);
        require(!noByok.allowed, "Remote provider must not consume an AIDao-funded credential/service path.");

        ProviderEconomicsPolicy.Decision byok = ProviderEconomicsPolicy.evaluate(remote, true, true);
        require(byok.allowed, "Explicitly selected BYOK remote provider should remain architecturally possible.");
        require(byok.fundingMode == ProviderEconomicsPolicy.FundingMode.USER_FUNDED_OPTIONAL,
                "Remote enhancement must remain user-funded/optional.");

        ModelProvider.Result plan = local.plan(new ModelProvider.Request(
                "Offline Sample",
                "Create a simple notes app with local persistence.",
                ""));
        require(plan.plan != null && !plan.plan.tasks.isEmpty(), "Zero-cost default path must remain functional.");
        require(!plan.remote, "Default planning result must report local execution.");

        System.out.println("PASS: zero-cost default provider economics and BYOK boundary verified");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
