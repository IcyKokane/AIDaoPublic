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

        ProviderRegistry registry = new ProviderRegistry();
        require(registry.selected() instanceof LocalPlanningProvider,
                "Registry must start on the zero-cost local provider.");
        registry.register(remote);
        require(!registry.select(remote.id()),
                "Legacy selection API must never silently select a remote provider.");
        require(registry.selected() instanceof LocalPlanningProvider,
                "Denied remote selection must leave the local provider active.");
        require(registry.lastSelectionDecision() != null && !registry.lastSelectionDecision().allowed,
                "Denied remote selection must expose a policy decision for UI diagnostics.");

        require(!registry.select(remote.id(), true, false),
                "Explicit remote opt-in without BYOK must remain blocked.");
        require(registry.selected() instanceof LocalPlanningProvider,
                "Blocked non-BYOK selection must not replace the local provider.");

        require(registry.select(remote.id(), true, true),
                "Explicit BYOK remote selection should be allowed as an optional enhancement.");
        require(registry.selected() == remote,
                "Authorized remote provider must become selected only after explicit BYOK authorization.");
        require(registry.lastSelectionDecision() != null
                        && registry.lastSelectionDecision().fundingMode == ProviderEconomicsPolicy.FundingMode.USER_FUNDED_OPTIONAL,
                "Authorized remote provider must remain classified as user-funded optional.");

        require(registry.select(local.id()),
                "Zero-cost local provider must always be selectable without credentials.");
        require(registry.selected() instanceof LocalPlanningProvider,
                "Registry must be able to return to the zero-cost core path.");

        ModelProvider.Result plan = local.plan(new ModelProvider.Request(
                "Offline Sample",
                "Create a simple notes app with local persistence.",
                ""));
        require(plan.plan != null && !plan.plan.tasks.isEmpty(), "Zero-cost default path must remain functional.");
        require(!plan.remote, "Default planning result must report local execution.");

        System.out.println("PASS: zero-cost default provider economics, registry selection gate, and BYOK boundary verified");
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
