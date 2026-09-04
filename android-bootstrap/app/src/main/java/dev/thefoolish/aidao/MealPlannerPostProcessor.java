package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Semantic product pass for offline recipe + weekly meal-planning requests.
 * The pass is deliberately deterministic and dependency-free: every requested
 * behavior is backed by generated Android source and LocalStore persistence.
 */
final class MealPlannerPostProcessor {
    static final class Result {
        final String projectName;
        final String packageName;
        final List<GeneratedProject.FileEntry> files;
        final List<String> notes;
        Result(String projectName,String packageName,List<GeneratedProject.FileEntry> files,List<String> notes){
            this.projectName=projectName;this.packageName=packageName;this.files=files;this.notes=notes;
        }
    }

    static Result process(String projectName,String packageName,List<GeneratedProject.FileEntry> incoming){
        List<GeneratedProject.FileEntry> source=incoming==null?new ArrayList<>():new ArrayList<>(incoming);
        String request=requestText(source).toLowerCase(Locale.US);
        String semantic=((projectName==null?"":projectName)+"\n"+request).toLowerCase(Locale.US);
        boolean recipes=any(semantic,"recipe","recipes","meal planner","meal planning");
        boolean planning=any(semantic,"schedule","weekly","week","days of the week","meal plan");
        boolean ingredients=any(semantic,"ingredient","ingredients","shopping list","grocery list");
        if(!(recipes&&planning&&ingredients)) return new Result(projectName,packageName,source,new ArrayList<>());

        String root="app/src/main/java/"+packageName.replace('.','/')+"/";
        source=retainExcept(source,root,"MainActivity.java","EditorActivity.java","SearchActivity.java","LibraryActivity.java");
        source.add(file(root+"MainActivity.java",weeklyPlan(packageName),"Render and mutate persisted weekly meal plan"));
        source.add(file(root+"EditorActivity.java",recipeEditor(packageName),"Create recipes with persisted ingredient lists"));
        source.add(file(root+"SearchActivity.java",shoppingList(packageName),"Derive deduplicated shopping list from scheduled meals"));
        source.add(file(root+"LibraryActivity.java",recipeLibrary(packageName),"Browse and favorite persisted recipes"));

        List<String> notes=new ArrayList<>();
        notes.add("PASS meal-planning intent generates recipe, weekly schedule, favorite, and derived shopping-list behavior");
        notes.add("PASS meal-planning state uses restart-safe LocalStore persistence");
        notes.add("PASS shopping list is computed from scheduled recipe ingredients rather than placeholder data");
        String name=projectName;
        String low=name==null?"":name.trim().toLowerCase(Locale.US);
        if(low.isEmpty()||low.startsWith("a ")||low.startsWith("an ")||low.startsWith("simple ")||low.contains("meal planning app")) name="MealMap";
        return new Result(name,packageName,source,notes);
    }

    private static String weeklyPlan(String p){return "package "+p+";\n"+
        "import android.widget.*;import java.util.*;public final class MainActivity extends AppScreen{private static final String[] DAYS={\"Monday\",\"Tuesday\",\"Wednesday\",\"Thursday\",\"Friday\",\"Saturday\",\"Sunday\"};protected void render(){title(\"Weekly meal plan\");String recipes=store.text(\"meal_recipes\",\"\");java.util.ArrayList<String> names=new java.util.ArrayList<>();names.add(\"No meal\");for(String row:recipes.split(\"\\n\")){String[] x=row.split(\"\\\\|\",2);if(x.length>0&&!x[0].trim().isEmpty())names.add(x[0]);}for(String day:DAYS){LinearLayout c=card(day,\"Choose a saved recipe\");Spinner pick=new Spinner(this);ArrayAdapter<String> a=new ArrayAdapter<>(this,android.R.layout.simple_spinner_item,names);a.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);pick.setAdapter(a);String current=store.text(\"meal_day_\"+day,\"No meal\");int pos=names.indexOf(current);pick.setSelection(pos<0?0:pos);pick.setContentDescription(day+\" meal selector\");pick.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener(){public void onNothingSelected(android.widget.AdapterView<?> p){}public void onItemSelected(android.widget.AdapterView<?> p,android.view.View v,int i,long id){store.putText(\"meal_day_\"+day,names.get(i));}});c.addView(pick);body.addView(c);}Button recipesButton=button(\"Recipes\");recipesButton.setOnClickListener(v->AppNavigator.open(this,LibraryActivity.class));body.addView(recipesButton);Button shopping=button(\"Shopping list\");shopping.setOnClickListener(v->AppNavigator.open(this,SearchActivity.class));body.addView(shopping);}}\n";}

    private static String recipeEditor(String p){return "package "+p+";\n"+
        "import android.widget.*;public final class EditorActivity extends AppScreen{protected void render(){title(\"New recipe\");EditText name=field(\"Recipe name\");EditText ingredients=field(\"Ingredients, comma separated\");ingredients.setMinLines(3);body.addView(name);body.addView(ingredients);Button save=button(\"Save recipe\");save.setOnClickListener(v->{String n=name.getText().toString().trim().replace(\"|\",\" \" ).replace(\"\\n\",\" \" );String ing=ingredients.getText().toString().trim().replace(\"|\",\" \" ).replace(\"\\n\",\",\");if(n.isEmpty()){name.setError(\"Enter a recipe name\");return;}if(ing.isEmpty()){ingredients.setError(\"Add at least one ingredient\");return;}String old=store.text(\"meal_recipes\",\"\");StringBuilder kept=new StringBuilder();for(String row:old.split(\"\\n\")){String[] x=row.split(\"\\\\|\",2);if(x.length>0&&!x[0].equalsIgnoreCase(n)&&!row.trim().isEmpty())kept.append(row).append(\"\\n\");}kept.append(n).append(\"|\").append(ing).append(\"\\n\");store.putText(\"meal_recipes\",kept.toString());Toast.makeText(this,\"Recipe saved\",Toast.LENGTH_SHORT).show();finish();});body.addView(save);}}\n";}

    private static String recipeLibrary(String p){return "package "+p+";\n"+
        "import android.widget.*;public final class LibraryActivity extends AppScreen{protected void render(){title(\"Recipes\");String raw=store.text(\"meal_recipes\",\"\");String fav=store.text(\"meal_favorites\",\"\");if(raw.trim().isEmpty())body.addView(card(\"No recipes yet\",\"Add a recipe with its ingredients to start planning.\"));for(String row:raw.split(\"\\n\")){if(row.trim().isEmpty())continue;String[] x=row.split(\"\\\\|\",2);String n=x[0],ing=x.length>1?x[1]:\"\";boolean favorite=(\"\\n\"+fav+\"\\n\").contains(\"\\n\"+n+\"\\n\");LinearLayout c=card(n,ing);Button star=button(favorite?\"Remove favorite\":\"Favorite\");star.setContentDescription((favorite?\"Remove \" : \"Add \" )+n+\" favorite\");star.setOnClickListener(v->{String f=store.text(\"meal_favorites\",\"\");if((\"\\n\"+f+\"\\n\").contains(\"\\n\"+n+\"\\n\"))f=f.replace(n+\"\\n\",\"\");else f+=n+\"\\n\";store.putText(\"meal_favorites\",f);recreate();});c.addView(star);body.addView(c);}Button add=button(\"Add recipe\");add.setOnClickListener(v->AppNavigator.open(this,EditorActivity.class));body.addView(add);}}\n";}

    private static String shoppingList(String p){return "package "+p+";\n"+
        "import java.util.*;public final class SearchActivity extends AppScreen{private static final String[] DAYS={\"Monday\",\"Tuesday\",\"Wednesday\",\"Thursday\",\"Friday\",\"Saturday\",\"Sunday\"};protected void render(){title(\"Shopping list\");String raw=store.text(\"meal_recipes\",\"\");LinkedHashMap<String,String> recipeIngredients=new LinkedHashMap<>();for(String row:raw.split(\"\\n\")){String[] x=row.split(\"\\\\|\",2);if(x.length==2)recipeIngredients.put(x[0],x[1]);}LinkedHashMap<String,Integer> items=new LinkedHashMap<>();for(String day:DAYS){String recipe=store.text(\"meal_day_\"+day,\"No meal\");String ing=recipeIngredients.get(recipe);if(ing==null)continue;for(String token:ing.split(\",\")){String item=token.trim();if(item.isEmpty())continue;String key=item.toLowerCase(java.util.Locale.US);items.put(key,items.containsKey(key)?items.get(key)+1:1);}}if(items.isEmpty())body.addView(card(\"Nothing to buy yet\",\"Schedule saved recipes and their ingredients will appear here automatically.\"));else{section(\"From this week's scheduled meals\");for(java.util.Map.Entry<String,Integer> e:items.entrySet())body.addView(card(e.getKey(),e.getValue()>1?\"Used by \"+e.getValue()+\" scheduled meals\":\"Used by 1 scheduled meal\"));}}}\n";}

    private static List<GeneratedProject.FileEntry> retainExcept(List<GeneratedProject.FileEntry> source,String root,String... names){List<GeneratedProject.FileEntry> out=new ArrayList<>();for(GeneratedProject.FileEntry f:source){if(f==null)continue;boolean drop=false;for(String n:names)if((root+n).equals(f.path)){drop=true;break;}if(!drop)out.add(f);}return out;}
    private static GeneratedProject.FileEntry file(String path,String content,String hint){return new GeneratedProject.FileEntry(path,content,hint);}
    private static boolean any(String s,String... terms){for(String t:terms)if(s.contains(t))return true;return false;}
    private static String requestText(List<GeneratedProject.FileEntry> files){for(GeneratedProject.FileEntry f:files)if(f!=null&&"README.md".equals(f.path)&&f.content!=null)return f.content;return "";}
}