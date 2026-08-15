package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Structured, local interpretation of an ordinary-language Android app brief. */
final class ProjectIntent {
    final String source;
    final String appKind;
    final List<String> screens;
    final List<String> entities;
    final Set<String> capabilities;

    private ProjectIntent(String source, String appKind, List<String> screens, List<String> entities, Set<String> capabilities) {
        this.source = source;
        this.appKind = appKind;
        this.screens = Collections.unmodifiableList(screens);
        this.entities = Collections.unmodifiableList(entities);
        this.capabilities = Collections.unmodifiableSet(capabilities);
    }

    static ProjectIntent parse(String brief, String context) {
        String s = ((brief == null ? "" : brief) + " " + (context == null ? "" : context))
                .toLowerCase(Locale.US).replaceAll("\\s+", " ").trim();
        Set<String> caps = new LinkedHashSet<>();
        List<String> screens = new ArrayList<>();
        List<String> entities = new ArrayList<>();
        String kind = "general";

        boolean anime = has(s,"anime","episode","mihon","watch history");
        boolean media = anime || has(s,"video","movie","stream","playback","player");
        boolean expense = has(s,"expense","budget","purchase","transaction","spending");
        boolean tracker = has(s,"track","tracker","activity","usage","habit");
        boolean social = has(s,"dating","match","profile","chat","message","friend");

        if (anime) {
            kind = "anime";
            add(screens,"Catalog","Search","Anime Detail","Player","Library","History","Providers","Settings");
            add(entities,"Anime","Episode","Provider","WatchProgress","Favorite");
            add(caps,"search","detail","favorites","history","playback","providers","local-storage","multi-screen","navigation");
        } else if (media) {
            kind = "media";
            add(screens,"Browse","Search","Media Detail","Player","Library","Settings");
            add(entities,"MediaItem","PlaybackProgress","Favorite");
            add(caps,"search","detail","favorites","playback","local-storage","multi-screen","navigation");
        } else if (expense) {
            kind = "finance-tracker";
            add(screens,"Dashboard","Transactions","Add Transaction","Budgets","Reports","Settings");
            add(entities,"Transaction","Category","Budget");
            add(caps,"forms","list","local-storage","search","totals","multi-screen","navigation");
        } else if (social) {
            kind = "social";
            add(screens,"Discover","Matches","Messages","Profile","Settings");
            add(entities,"Profile","Match","Message");
            add(caps,"profiles","list","forms","local-storage","multi-screen","navigation");
        } else if (tracker) {
            kind = "tracker";
            add(screens,"Dashboard","Activity","Reports","Settings");
            add(entities,"ActivityRecord","Report");
            add(caps,"list","local-storage","totals","multi-screen","navigation");
        } else {
            add(screens,"Home","Details","Settings");
            add(entities,"Item");
            add(caps,"multi-screen","navigation","local-storage");
        }

        if (has(s,"login","sign in","account","oauth")) add(caps,"authentication");
        if (has(s,"github")) add(caps,"github");
        if (has(s,"download","upload","import","export","file")) add(caps,"files");
        if (has(s,"notification","notify","alert")) add(caps,"notifications");
        if (has(s,"location","route","map","gps")) add(caps,"location");
        if (has(s,"ai","assistant","model","generate")) add(caps,"model-provider");
        if (has(s,"repository","extension","plugin","provider","source")) add(caps,"providers");
        if (has(s,"offline","local","device")) add(caps,"local-storage");
        if (has(s,"favorite","favourite","library")) add(caps,"favorites");
        if (has(s,"search")) add(caps,"search");
        if (has(s,"history","recent")) add(caps,"history");
        return new ProjectIntent(s,kind,screens,entities,caps);
    }

    boolean has(String capability) { return capabilities.contains(capability); }

    private static boolean has(String source,String... terms){for(String t:terms)if(source.contains(t))return true;return false;}
    private static void add(List<String> out,String... values){for(String v:values)if(!out.contains(v))out.add(v);}
    private static void add(Set<String> out,String... values){Collections.addAll(out,values);}
}
