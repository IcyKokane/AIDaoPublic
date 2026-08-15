package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Safe local planning layer. It turns ordinary-language briefs plus accumulated
 * project context into an editable implementation specification without network
 * calls, credential use, installation, publication, or destructive actions.
 * Planning/source stages never install APKs; execution gates remain explicit.
 */
public final class ProjectPlanner {
    public static final class Plan {
        public final List<String> requirements;
        public final List<String> tasks;
        public final List<String> assumptions;
        Plan(List<String> requirements,List<String> tasks,List<String> assumptions){
            this.requirements=Collections.unmodifiableList(requirements);
            this.tasks=Collections.unmodifiableList(tasks);
            this.assumptions=Collections.unmodifiableList(assumptions);
        }
    }

    private ProjectPlanner() {}

    public static Plan build(String brief,String context){
        String base=normalize(brief);
        String refinement=normalize(context);
        String source=normalize(base+" "+refinement);
        Set<String> requirements=new LinkedHashSet<>();
        Set<String> tasks=new LinkedHashSet<>();
        List<String> assumptions=new ArrayList<>();

        requirements.add("Provide an Android-native application with persistent project state, clear navigation, loading/empty/error states, and accessible touch targets.");
        tasks.add("Create the Android application shell, reusable theme/components, navigation model, and persistent project-level state.");

        boolean anime=enabled(source,refinement,new String[]{"anime","episode","manga","watch anime"});
        boolean media=anime||enabled(source,refinement,new String[]{"video","media","stream","player","playback","music","podcast"});
        boolean providers=enabled(source,refinement,new String[]{"plugin","extension","provider","repository","repo","source"});
        boolean social=enabled(source,refinement,new String[]{"chat","message","friend","profile","social","community","dating"});
        boolean tracker=enabled(source,refinement,new String[]{"track","tracker","history","report","analytics","usage","activity","habit"});
        boolean finance=enabled(source,refinement,new String[]{"expense","budget","purchase","transaction","finance","money","spending"});
        boolean commerce=enabled(source,refinement,new String[]{"shop","store","cart","product","checkout","marketplace","order"});
        boolean content=enabled(source,refinement,new String[]{"note","document","article","post","journal","editor","write","content"});

        if(anime){
            requirement(requirements,"Provide an anime catalog with search/browse, details, episode lists, favorites/library, watch history, and resume progress.");
            requirement(requirements,"Keep anime metadata and episode discovery behind replaceable provider interfaces so one failing source cannot break healthy providers.");
            requirement(requirements,"Expose provider availability, loading, empty, disabled, and failure states instead of silently hiding source errors.");
            task(tasks,"Define anime, episode, provider, library, history, and watch-progress domain models.");
            task(tasks,"Implement provider contracts for catalog search, anime details, episode discovery, and stream resolution.");
            task(tasks,"Build separate Catalog, Anime Detail, Library, History, Player, and Provider Management screens with navigation between them.");
            task(tasks,"Implement playback state, explicit stream selection, resume position, fullscreen/orientation handling, and visible playback errors.");
            task(tasks,"Persist favorites, watch history, episode progress, and recent activity locally on-device.");
            task(tasks,"Add provider failure isolation and allow enable/disable/provider switching without affecting unrelated sources.");
        } else if(media){
            requirement(requirements,"Provide media browse/search, detail, playback, favorites/history, and visible provider/error states.");
            task(tasks,"Define media catalog, detail, playback, favorites/history, and provider models.");
            task(tasks,"Build separate Browse, Detail, Library/History, and Player screens connected through explicit navigation.");
            task(tasks,"Implement playback/resume state and provider-isolated failure handling.");
        }

        if(finance){
            requirement(requirements,"Support transaction entry, categories, budgets, summaries, search/filtering, and durable local history.");
            task(tasks,"Define transaction, category, recurring-expense, and budget data models.");
            task(tasks,"Build Dashboard, Transactions, Budgets, and Reports screens with explicit navigation.");
            task(tasks,"Persist financial records locally and compute daily/monthly totals without requiring a remote account.");
        }
        if(tracker){
            requirement(requirements,"Capture user-approved activity records with useful daily/weekly/monthly summaries and transparent data ownership.");
            task(tasks,"Define activity/event, aggregation, retention, and report models.");
            task(tasks,"Build Overview, Timeline, Reports, and Data Controls screens with persistent local state.");
        }
        if(social){
            requirement(requirements,"Provide profile, conversation/list, detail, and relationship/community flows with explicit loading/error states.");
            task(tasks,"Define profile, conversation/message, and relationship/community models.");
            task(tasks,"Build multi-screen Home, Inbox, Profile, and Settings flows.");
        }
        if(commerce){
            requirement(requirements,"Provide product discovery, product detail, cart/selection state, and order/checkout preparation without hidden spending.");
            task(tasks,"Define product, cart, selection, and order-intent models.");
            task(tasks,"Build Catalog, Product Detail, Cart, and Orders screens; keep final spending as an explicit user-controlled action.");
        }
        if(content){
            requirement(requirements,"Support creating, editing, viewing, searching, and locally persisting user-authored content.");
            task(tasks,"Define content/document models and local persistence.");
            task(tasks,"Build Home, Editor, Search, and Library screens with unsaved-change protection.");
        }

        if(!anime&&!media&&!finance&&!tracker&&!social&&!commerce&&!content){
            task(tasks,"Infer the primary domain objects from the brief and implement at least four connected screens rather than a static requirements page.");
            task(tasks,"Implement the primary feature flow with explicit data-state boundaries and local persistence where useful.");
        }

        feature(source,refinement,requirements,tasks,new String[]{"login","account","sign in","oauth"},
                "Support authentication/account state through an explicit user-controlled sign-in flow.",
                "Add sign-in/account screens, session state, logout, and secure credential boundaries.");
        feature(source,refinement,requirements,tasks,new String[]{"github"},
                "Integrate with a user-controlled GitHub repository.",
                "Add repository connection, synchronization status, permission diagnostics, and explicit send/build controls.");
        feature(source,refinement,requirements,tasks,new String[]{"download","file","upload","import","export"},
                "Allow user-controlled file import/export where required using Android-scoped storage.",
                "Implement document-picker based file import/export and validate imported content before use.");
        if(providers&&!anime) feature(source,refinement,requirements,tasks,new String[]{"plugin","extension","provider","repository","repo","source"},
                "Support replaceable provider/plugin-style data sources behind a stable app-owned interface.",
                "Define provider contracts, provider discovery metadata, enable/disable state, and provider failure isolation.");
        feature(source,refinement,requirements,tasks,new String[]{"offline","local","device"},
                "Keep useful app data available locally on-device with explicit ownership/clearing controls.",
                "Add durable local persistence and recovery after process/app restart.");
        feature(source,refinement,requirements,tasks,new String[]{"notification","notify","alert"},
                "Use user-visible Android notifications only after permission and settings are explicitly enabled.",
                "Add notification channels, permission handling, and per-feature notification controls.");
        feature(source,refinement,requirements,tasks,new String[]{"location","route","map","gps"},
                "Use location only with explicit Android permission, visible indication, and user control.",
                "Implement permission-gated location access and an isolated route/location service.");
        feature(source,refinement,requirements,tasks,new String[]{"ai","model","assistant","generate"},
                "Expose AI-assisted behavior through a visible provider boundary with request state, errors, and approval points.",
                "Define a model-provider interface with a safe local/default implementation and explicit external-provider configuration.");

        addExplicitPreference(source,requirements,"dark","Use a dark-first visual theme while preserving contrast and accessibility.");
        addExplicitPreference(source,requirements,"light theme","Support a light theme option without removing accessible contrast.");
        addExplicitPreference(source,requirements,"search","Provide a visible search/filter interaction appropriate to the primary data model.");
        addExplicitPreference(source,requirements,"favorite","Persist user favorites/bookmarks locally unless the project explicitly requires account sync.");
        addExplicitPreference(source,requirements,"history","Expose a user-visible history/recent activity surface where the domain supports it.");

        task(tasks,"Generate resources, manifest declarations, multiple Android screens, navigation wiring, and reusable UI/data architecture that reflect the inferred feature set.");
        task(tasks,"Add deterministic verification for required files, manifest/navigation consistency, persistence wiring, and the primary user flow.");
        task(tasks,"Run Android CI, diagnose failures, apply bounded source/build repairs, and produce an installable debug APK only after verification succeeds.");

        if(source.isEmpty()) assumptions.add("The project brief is incomplete; planning remains a safe Android baseline until more context is supplied.");
        else assumptions.add("Requirements are inferred from ordinary-language project context and remain editable before implementation/build actions.");
        if(!refinement.isEmpty()) assumptions.add("Later refinement context is treated as higher priority than the original brief when it explicitly removes or replaces a feature.");
        if(anime||providers) assumptions.add("Provider architecture is a technical boundary; AIDao does not assume an unverified content source or repository is safe or available.");
        assumptions.add("Imported/shared material is treated as data/knowledge unless the user explicitly authorizes a separate safe execution action.");
        assumptions.add("Installation, external publishing, spending, credential use, and destructive actions remain user-controlled.");
        return new Plan(new ArrayList<>(requirements),new ArrayList<>(tasks),assumptions);
    }

    private static boolean enabled(String source,String refinement,String[] terms){
        for(String term:terms){
            String t=term.toLowerCase(Locale.US);
            if(!source.contains(t)) continue;
            if(isNegated(refinement,t)) continue;
            return true;
        }
        return false;
    }
    private static boolean isNegated(String refinement,String term){
        if(refinement==null||refinement.isEmpty())return false;
        String[] prefixes={"remove ","remove the ","without ","no ","do not include ","don't include ","disable ","replace "+term+" with ","instead of "};
        for(String p:prefixes)if(refinement.contains(p+term)||("replace "+term+" with ").equals(p)&&refinement.contains(p))return true;
        return false;
    }
    private static void requirement(Set<String>s,String v){s.add(v);}
    private static void task(Set<String>s,String v){s.add(v);}
    private static void feature(String source,String refinement,Set<String>requirements,Set<String>tasks,String[]terms,String requirement,String task){if(enabled(source,refinement,terms)){requirements.add(requirement);tasks.add(task);}}
    private static void addExplicitPreference(String source,Set<String>requirements,String term,String requirement){if(source.contains(term))requirements.add(requirement);}
    private static String normalize(String value){return value==null?"":value.toLowerCase(Locale.US).replaceAll("\\s+"," ").trim();}
}
