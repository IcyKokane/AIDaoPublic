package dev.thefoolish.aidao;

import java.util.Arrays;

/** Quality regression for a nontrivial restart-safe meal-planning product. */
public final class MealPlannerSemanticAcceptance {
    public static void main(String[] args) {
        GeneratedProject project = new LocalSourceGenerator().generate(
                "A meal planning app",
                "Create an offline meal planning app where I can save recipes with ingredients, schedule recipes onto days of the week, favorite recipes, and automatically build a deduplicated shopping list from the scheduled meals. Data must survive restarts.",
                Arrays.asList(
                        "Save recipes with ingredients locally",
                        "Schedule saved recipes Monday through Sunday",
                        "Favorite and unfavorite recipes",
                        "Build the shopping list from scheduled recipe ingredients",
                        "Deduplicate repeated shopping ingredients",
                        "Keep recipes, favorites, and schedule after restart"),
                Arrays.asList("Generate real product behavior", "Validate semantics", "Build Android APK"));

        String all = allText(project);
        require(!hasFail(project), "meal planner generation failed verification: " + firstFail(project));
        require("MealMap".equals(project.projectName), "descriptor identity was not normalized to a concise app name: " + project.projectName);
        require(all.contains("meal_recipes"), "recipes have no dedicated persisted state");
        require(all.contains("meal_day_") && all.contains("Monday") && all.contains("Sunday"), "weekly schedule is not executable");
        require(all.contains("meal_favorites") && all.contains("Favorite"), "favorite mutation is not executable");
        require(all.contains("LinkedHashMap<String,Integer>") && all.contains("scheduled meals"), "shopping list is not dynamically aggregated/deduplicated");
        require(all.contains("Recipe name") && all.contains("Ingredients, comma separated") && all.contains("Save recipe"), "recipe creation UI is incomplete");
        require(all.contains("store.putText(\"meal_recipes\"") && all.contains("store.text(\"meal_recipes\""), "recipe state is not restart-safe");
        require(all.contains("setOnApplyWindowInsetsListener"), "meal planner is missing phone-safe inset handling");
        require(all.contains("setContentDescription"), "meal planner is missing accessibility descriptions");
        require(!all.toLowerCase().contains("todo: implement") && !all.toLowerCase().contains("coming soon"), "meal planner contains placeholder completion language");

        System.out.println("Meal planner semantic acceptance passed: recipes/schedule/favorites/derived shopping list are executable and restart-safe.");
    }

    private static String allText(GeneratedProject project) {
        StringBuilder out = new StringBuilder();
        for (GeneratedProject.FileEntry file : project.files) if (file != null && file.content != null) out.append('\n').append(file.content);
        return out.toString();
    }

    private static boolean hasFail(GeneratedProject project) {
        for (String note : project.verificationNotes) if (note != null && note.startsWith("FAIL ")) return true;
        return false;
    }

    private static String firstFail(GeneratedProject project) {
        for (String note : project.verificationNotes) if (note != null && note.startsWith("FAIL ")) return note;
        return "unknown";
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }
}
