package dev.thefoolish.aidao;

/** Default offline provider. It is deterministic, requires no API key, and performs no writes. */
public final class LocalPlanningProvider implements ModelProvider {
    @Override public String id() { return "local-planner"; }
    @Override public String displayName() { return "AIDao Local Planner"; }
    @Override public boolean isRemote() { return false; }

    @Override public Result plan(Request request) {
        ProjectPlanner.Plan plan = ProjectPlanner.build(request.brief, request.context);
        String summary = "Generated " + plan.requirements.size() + " requirements and " + plan.tasks.size() + " implementation tasks locally.";
        return new Result(plan, displayName(), false, summary);
    }
}
