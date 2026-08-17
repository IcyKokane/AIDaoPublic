package dev.thefoolish.generated.makeananimeapplikemihonit;

import java.util.*;
/** Safe placeholder provider used until the user explicitly configures a trusted provider implementation. */
public final class DemoProvider implements MediaProvider { public String id(){return "demo";}public String displayName(){return "Demo provider";}public boolean enabled(){return true;}public String health(){return "Ready (sample data only)";} public List<AnimeItem> search(String q){List<AnimeItem> all=Arrays.asList(new AnimeItem("origin","Origin Path","Sample catalog entry proving catalog/detail/player navigation.",id(),12),new AnimeItem("sky","Sky Archive","Second sample entry for search and library state.",id(),24));if(q==null||q.trim().isEmpty())return all;List<AnimeItem> out=new ArrayList<>();for(AnimeItem a:all)if(a.title.toLowerCase().contains(q.toLowerCase()))out.add(a);return out;} }
