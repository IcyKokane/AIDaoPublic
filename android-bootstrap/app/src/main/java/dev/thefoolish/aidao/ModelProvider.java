package dev.thefoolish.aidao;

/**
 * Boundary for AI-backed project understanding. Implementations must return a plan only;
 * they never install APKs, publish code, spend money, or perform destructive actions.
 */
public interface ModelProvider {
    final class Request {
        public final String projectName;
        public final String brief;
        public final String context;

        public Request(String projectName, String brief, String context) {
            this.projectName = projectName == null ? "" : projectName;
            this.brief = brief == null ? "" : brief;
            this.context = context == null ? "" : context;
        }
    }

    final class Result {
        public final ProjectPlanner.Plan plan;
        public final String providerName;
        public final boolean remote;
        public final String summary;

        public Result(ProjectPlanner.Plan plan, String providerName, boolean remote, String summary) {
            this.plan = plan;
            this.providerName = providerName;
            this.remote = remote;
            this.summary = summary;
        }
    }

    String id();
    String displayName();
    boolean isRemote();
    Result plan(Request request) throws Exception;
}
