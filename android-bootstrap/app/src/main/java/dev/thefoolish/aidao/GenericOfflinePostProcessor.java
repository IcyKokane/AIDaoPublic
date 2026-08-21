package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Converts simple offline list requests that do not map to a richer domain into
 * a real persisted local product instead of leaving the legacy generic sample-state shell.
 */
final class GenericOfflinePostProcessor {
    static final class Result {
        final String projectName;
        final String packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName, String packageName, List<GeneratedProject.FileEntry> files, List<String> notes) {
            this.projectName = projectName;
            this.packageName = packageName;
            this.files = files;
            this.notes = notes;
        }
    }

    static Result process(String projectName, String packageName, List<GeneratedProject.FileEntry> incoming) {
        List<GeneratedProject.FileEntry> source = incoming == null ? new ArrayList<>() : new ArrayList<>(incoming);
        if (hasSuffix(source, "/MediaProvider.java") || hasSuffix(source, "/AnimeItem.java"))
            return new Result(projectName, packageName, source, new ArrayList<>());

        String request = requestText(source).toLowerCase(Locale.US);
        boolean offlineList = any(request, "grocery list", "shopping list", "checklist", "to-do list", "todo list")
                && any(request, "offline", "persist", "restart", "keep the list", "save");
        if (!offlineList) return new Result(projectName, packageName, source, new ArrayList<>());

        String root = "app/src/main/java/" + packageName.replace('.', '/') + "/";
        List<GeneratedProject.FileEntry> out = new ArrayList<>();
        for (GeneratedProject.FileEntry f : source) {
            if (f == null) continue;
            String p = f.path;
            if (p.equals(root + "MainActivity.java") || p.equals(root + "ExploreActivity.java")
                    || p.equals(root + "DetailActivity.java") || p.equals(root + "SettingsActivity.java")) continue;
            out.add(f);
        }
        out.add(file(root + "MainActivity.java", main(packageName), "Render persisted grocery items"));
        out.add(file(root + "ExploreActivity.java", editor(packageName), "Add and edit persisted grocery items"));
        out.add(file(root + "DetailActivity.java", summary(packageName), "Summarize persisted grocery items"));
        out.add(file(root + "SettingsActivity.java", settings(packageName), "Provide explicit local list data controls"));

        List<String> notes = new ArrayList<>();
        notes.add("PASS generic offline list request replaced sample-state placeholders with persisted add/edit behavior");
        notes.add("PASS generic offline list uses putText mutations and restart-safe text reads");
        return new Result(projectName, packageName, out, notes);
    }

    private static GeneratedProject.FileEntry file(String p, String c, String h) {
        return new GeneratedProject.FileEntry(p, c, h);
    }
    private static boolean hasSuffix(List<GeneratedProject.FileEntry> files, String suffix) {
        for (GeneratedProject.FileEntry f : files) if (f != null && f.path != null && f.path.endsWith(suffix)) return true;
        return false;
    }
    private static boolean any(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }
    private static String requestText(List<GeneratedProject.FileEntry> files) {
        for (GeneratedProject.FileEntry f : files)
            if (f != null && "README.md".equals(f.path) && f.content != null) return f.content;
        StringBuilder b = new StringBuilder();
        for (GeneratedProject.FileEntry f : files) if (f != null && f.content != null) b.append('\n').append(f.content);
        return b.toString();
    }

    private static String main(String p) {
        return "package " + p + ";\n" +
                "import android.widget.*;\n" +
                "public final class MainActivity extends GeneratedScreen{" +
                "protected void render(){store.putText(\"last_surface\",\"list\");body.addView(text(\"Grocery List\",22,true));" +
                "String raw=store.text(\"grocery_items\",\"\");if(raw.trim().isEmpty())body.addView(text(\"No items yet. Add your first item.\",14,false));" +
                "else{int n=1;for(String item:raw.split(\"\\n\")){if(item.trim().isEmpty())continue;body.addView(text(n+\". \"+item,16,false));n++;}}" +
                "Button edit=action(\"Add or edit items\");edit.setOnClickListener(v->AppNavigator.open(this,ExploreActivity.class));body.addView(edit);" +
                "Button summary=action(\"List summary\");summary.setOnClickListener(v->AppNavigator.open(this,DetailActivity.class));body.addView(summary);" +
                "Button settings=action(\"Data controls\");settings.setOnClickListener(v->AppNavigator.open(this,SettingsActivity.class));body.addView(settings);}}\n";
    }

    private static String editor(String p) {
        return "package " + p + ";\n" +
                "import android.widget.*;import java.util.*;\n" +
                "public final class ExploreActivity extends GeneratedScreen{" +
                "protected void render(){store.putText(\"last_surface\",\"editor\");body.addView(text(\"Add or edit item\",22,true));" +
                "EditText item=new EditText(this);item.setHint(\"Item name\");body.addView(item);EditText number=new EditText(this);number.setHint(\"Item number to replace (optional)\");number.setInputType(2);body.addView(number);" +
                "Button save=action(\"Save item\");save.setOnClickListener(v->{String name=item.getText().toString().trim();if(name.isEmpty()){item.setError(\"Enter an item\");return;}" +
                "List<String> rows=new ArrayList<>();String raw=store.text(\"grocery_items\",\"\");if(!raw.trim().isEmpty())for(String x:raw.split(\"\\n\"))if(!x.trim().isEmpty())rows.add(x);" +
                "String n=number.getText().toString().trim();if(!n.isEmpty()){try{int i=Integer.parseInt(n)-1;if(i<0||i>=rows.size()){number.setError(\"Choose an existing item number\");return;}rows.set(i,name);}catch(Exception e){number.setError(\"Use a valid item number\");return;}}else rows.add(name);" +
                "store.putText(\"grocery_items\",join(rows));item.setText(\"\");number.setText(\"\");Toast.makeText(this,\"List saved\",Toast.LENGTH_SHORT).show();});body.addView(save);" +
                "Button back=action(\"View list\");back.setOnClickListener(v->AppNavigator.open(this,MainActivity.class));body.addView(back);}" +
                "private String join(List<String> rows){StringBuilder b=new StringBuilder();for(String x:rows){if(b.length()>0)b.append(\"\\n\");b.append(x.replace(\"\\n\",\" \").trim());}return b.toString();}}\n";
    }

    private static String summary(String p) {
        return "package " + p + ";\n" +
                "public final class DetailActivity extends GeneratedScreen{protected void render(){store.putText(\"last_surface\",\"summary\");body.addView(text(\"List summary\",22,true));String raw=store.text(\"grocery_items\",\"\");int count=0;if(!raw.trim().isEmpty())for(String x:raw.split(\"\\n\"))if(!x.trim().isEmpty())count++;body.addView(text(count+\" saved item\"+(count==1?\"\":\"s\"),16,false));}}\n";
    }

    private static String settings(String p) {
        return "package " + p + ";\n" +
                "import android.widget.*;\n" +
                "public final class SettingsActivity extends GeneratedScreen{protected void render(){store.putText(\"last_surface\",\"settings\");body.addView(text(\"Data controls\",22,true));body.addView(text(\"Your grocery list stays on this device.\",14,false));" +
                "Button clear=action(store.flag(\"confirm_clear_grocery\")?\"Tap again to clear list\":\"Clear grocery list\");clear.setOnClickListener(v->{if(!store.flag(\"confirm_clear_grocery\")){store.flag(\"confirm_clear_grocery\",true);recreate();return;}store.putText(\"grocery_items\",\"\");store.flag(\"confirm_clear_grocery\",false);Toast.makeText(this,\"List cleared\",Toast.LENGTH_SHORT).show();});body.addView(clear);}}\n";
    }

    private GenericOfflinePostProcessor() {}
}
