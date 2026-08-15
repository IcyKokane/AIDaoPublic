package dev.thefoolish.aidao;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

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

        boolean anime=enabled(source,refinement,new String[]{"anime","episode","episodes","manga","watch anime"});
        boolean media=anime||enabled(source,refinement,new String[]{"video","videos","media","stream","streaming","player","playback","music","podcast","podcasts"});
        boolean providers=enabled(source,refinement,new String[]{"plugin","plugins","extension","extensions","provider","providers","repository","repositories","repo","source","sources"});
        boolean social=enabled(source,refinement,new String[]{"chat","message","messages","friend","friends","profile","profiles","social","community","dating"});
        boolean tracker=enabled(source,refinement,new String[]{"track","tracker","tracking","report","reports","analytics","usage","activity","activities","habit","habits"});
        boolean finance=enabled(source,refinement,new String[]{"expense","expenses","budget","budgets","purchase","purchases","transaction","transactions","finance","money","spending"});
        boolean commerce=enabled(source,refinement,new String[]{"shop","store","cart","product","products","checkout","marketplace","order","orders"});
        boolean content=enabled(source,refinement,new String[]{"note","notes","document","documents","article","articles","post","posts","journal","editor","write","writing","content"});
        boolean remoteData=enabled(source,refinement,new String[]{"api","server","servers","backend","cloud","remote","web service","web services","network","feed","feeds","sync"});
        boolean camera=enabled(source,refinement,new String[]{"camera","photo","photos","picture","pictures","scan","scanner","qr code","qr codes","barcode","barcodes"});
        boolean notifications=enabled(source,refinement,new String[]{"notification","notifications","notify","alert","alerts","reminder","reminders"});
        boolean location=enabled(source,refinement,new String[]{"location","locations","route","routes","map","maps","gps","geofence","geofencing"});
        boolean background=enabled(source,refinement,new String[]{"background","periodic","scheduled","schedule","sync every","daily sync","worker","workers"});
        boolean bluetooth=enabled(source,refinement,new String[]{"bluetooth","ble","nearby device","nearby devices","wearable","wearables"});

        boolean searchAllowed=!negatedAny(refinement,new String[]{"search","filter","browse"});
        boolean favoritesAllowed=!negatedAny(refinement,new String[]{"favorite","favorites","bookmark","bookmarks","library"});
        boolean historyAllowed=!negatedAny(refinement,new String[]{"history","watch history","recent activity"});
        boolean resumeAllowed=!negatedAny(refinement,new String[]{"resume","resume progress","watch progress","progress"});
        boolean playbackAllowed=!negatedAny(refinement,new String[]{"playback","player","watch","stream"});

        if(anime){
            requirement(requirements,"Provide an anime catalog with details and episode lists using multiple Android screens.");
            if(searchAllowed) requirement(requirements,"Provide anime catalog search/browse with visible loading, empty, and error states.");
            if(favoritesAllowed) requirement(requirements,"Provide a persistent favorites/library surface for anime selected by the user.");
            if(historyAllowed) requirement(requirements,"Persist watch history and expose it through a user-visible history surface.");
            if(resumeAllowed) requirement(requirements,"Persist per-episode watch progress so playback can resume where the user stopped.");
            requirement(requirements,"Keep anime metadata and episode discovery behind replaceable provider interfaces so one failing source cannot break healthy providers.");
            requirement(requirements,"Expose provider availability, loading, empty, disabled, and failure states instead of silently hiding source errors.");
            task(tasks,"Define anime, episode, provider, library, history, and watch-progress domain models while omitting any explicitly removed optional surfaces.");
            task(tasks,"Implement provider contracts for catalog search, anime details, episode discovery, and stream resolution.");
            StringBuilder screens=new StringBuilder("Build separate Catalog and Anime Detail screens");
            if(favoritesAllowed)screens.append(", Library");
            if(historyAllowed)screens.append(", History");
            if(playbackAllowed)screens.append(", Player");
            screens.append(", and Provider Management screens with navigation between enabled surfaces.");
            task(tasks,screens.toString());
            if(playbackAllowed) task(tasks,"Implement playback state, explicit stream selection, fullscreen/orientation handling, and visible playback errors"+(resumeAllowed?", including resume position.":"."));
            if(favoritesAllowed||historyAllowed||resumeAllowed){
                List<String> state=new ArrayList<>();
                if(favoritesAllowed)state.add("favorites");
                if(historyAllowed)state.add("watch history");
                if(resumeAllowed)state.add("episode progress");
                task(tasks,"Persist "+joinNatural(state)+" locally on-device.");
            }
            task(tasks,"Add provider failure isolation and allow enable/disable/provider switching without affecting unrelated sources.");
        } else if(media){
            requirement(requirements,"Provide media detail and visible provider/error states across multiple Android screens.");
            if(searchAllowed) requirement(requirements,"Provide media browse/search appropriate to the primary catalog.");
            if(favoritesAllowed||historyAllowed) requirement(requirements,"Provide locally persistent "+(favoritesAllowed?"favorites":"")+(favoritesAllowed&&historyAllowed?" and ":"")+(historyAllowed?"history":"")+" state.");
            task(tasks,"Define media catalog, detail, playback, favorites/history, and provider models while honoring explicitly removed optional features.");
            task(tasks,"Build connected media Browse/Detail screens plus only the enabled Library, History, and Player surfaces.");
            if(playbackAllowed) task(tasks,"Implement playback state and provider-isolated failure handling"+(resumeAllowed?" with resume progress.":"."));
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
        feature(source,refinement,requirements,tasks,new String[]{"download","downloads","file","files","upload","uploads","import","imports","export","exports"},
                "Allow user-controlled file import/export where required using Android-scoped storage.",
                "Implement document-picker based file import/export and validate imported content before use.");
        if(providers&&!anime) feature(source,refinement,requirements,tasks,new String[]{"plugin","plugins","extension","extensions","provider","providers","repository","repositories","repo","source","sources"},
                "Support replaceable provider/plugin-style data sources behind a stable app-owned interface.",
                "Define provider contracts, provider discovery metadata, enable/disable state, and provider failure isolation.");
        feature(source,refinement,requirements,tasks,new String[]{"offline","local","device"},
                "Keep useful app data available locally on-device with explicit ownership/clearing controls.",
                "Add durable local persistence and recovery after process/app restart.");
        feature(source,refinement,requirements,tasks,new String[]{"ai","model","models","assistant","generate","generation"},
                "Expose AI-assisted behavior through a visible provider boundary with request state, errors, and approval points.",
                "Define a model-provider interface with a safe local/default implementation and explicit external-provider configuration.");

        if(remoteData){
            requirement(requirements,"Keep remote/network data access behind an app-owned gateway with explicit request, loading, retry, timeout, and failure states.");
            task(tasks,"Add an isolated network/data gateway, connectivity-aware request state, bounded retries, and a deterministic offline fallback or empty state.");
            assumptions.add("Remote endpoints, API keys, accounts, and credentials are not invented by AIDao; they require explicit project configuration or user-provided values.");
        }
        if(camera){
            requirement(requirements,"Request camera/media access only at the moment the related feature is used and provide a functional denied-permission state.");
            task(tasks,"Add permission-gated camera/media capture or picker flow with validation and a non-camera fallback where practical.");
        }
        if(notifications){
            requirement(requirements,"Use user-visible Android notifications only after permission and settings are explicitly enabled.");
            task(tasks,"Add notification channels, Android 13+ permission handling, per-feature notification controls, and a visible disabled state.");
        }
        if(location){
            requirement(requirements,"Use location only with explicit Android permission, visible indication, and user control.");
            task(tasks,"Implement permission-gated location access and an isolated route/location service with unavailable/denied states.");
        }
        if(background){
            requirement(requirements,"Run scheduled/background work through Android-supported constrained jobs without hidden indefinite background execution.");
            task(tasks,"Add WorkManager-style scheduling boundaries, network/battery constraints where applicable, retry limits, and a user-visible enable/disable control.");
        }
        if(bluetooth){
            requirement(requirements,"Use Bluetooth/Nearby Devices only after explicit runtime permission and expose connection/disconnection state.");
            task(tasks,"Add a permission-gated Bluetooth abstraction with scan/connect state, timeout handling, and no silent pairing actions.");
        }

        addExplicitPreference(source,refinement,requirements,"dark","Use a dark-first visual theme while preserving contrast and accessibility.");
        addExplicitPreference(source,refinement,requirements,"light theme","Support a light theme option without removing accessible contrast.");
        addExplicitPreference(source,refinement,requirements,"search","Provide a visible search/filter interaction appropriate to the primary data model.");
        addExplicitPreference(source,refinement,requirements,"favorite","Persist user favorites/bookmarks locally unless the project explicitly requires account sync.");
        addExplicitPreference(source,refinement,requirements,"history","Expose a user-visible history/recent activity surface where the domain supports it.");

        task(tasks,"Generate resources, manifest declarations, multiple Android screens, navigation wiring, and reusable UI/data architecture that reflect the inferred feature set.");
        task(tasks,"Add deterministic verification for required files, manifest/navigation consistency, persistence wiring, declared permissions, and the primary user flow.");
        task(tasks,"Run Android CI, diagnose failures, apply bounded source/build repairs, and produce an installable debug APK only after verification succeeds.");

        if(source.isEmpty()) assumptions.add("The project brief is incomplete; planning remains a safe Android baseline until more context is supplied.");
        else assumptions.add("Requirements are inferred from ordinary-language project context and remain editable before implementation/build actions.");
        if(!refinement.isEmpty()) assumptions.add("Later refinement context is treated as higher priority than the original brief when it explicitly removes or replaces a feature.");
        if(anime||providers) assumptions.add("Provider architecture is a technical boundary; AIDao does not assume an unverified content source or repository is safe or available.");
        assumptions.add("Runtime permissions are requested only for features the project actually requires; permission denial must leave the app in an understandable non-crashing state.");
        assumptions.add("Imported/shared material is treated as data/knowledge unless the user explicitly authorizes a separate safe execution action.");
        assumptions.add("Installation, external publishing, spending, credential use, and destructive actions remain user-controlled.");
        return new Plan(new ArrayList<>(requirements),new ArrayList<>(tasks),assumptions);
    }

    private static boolean enabled(String source,String refinement,String[] terms){
        for(String term:terms){
            String t=term.toLowerCase(Locale.US);
            if(!containsTerm(source,t)) continue;
            if(isNegated(refinement,t)) continue;
            return true;
        }
        return false;
    }
    private static boolean containsTerm(String source,String term){
        if(source==null||source.isEmpty()||term==null||term.isEmpty())return false;
        return Pattern.compile("(?<![a-z0-9])"+Pattern.quote(term)+"(?![a-z0-9])").matcher(source).find();
    }
    private static boolean negatedAny(String refinement,String[]terms){for(String term:terms)if(isNegated(refinement,term.toLowerCase(Locale.US)))return true;return false;}
    private static boolean isNegated(String refinement,String term){
        if(refinement==null||refinement.isEmpty())return false;
        String[] prefixes={"remove ","remove the ","without ","no ","do not include ","don't include ","disable ","replace "+term+" with ","instead of "};
        for(String p:prefixes)if(refinement.contains(p+term)||(("replace "+term+" with ").equals(p)&&refinement.contains(p)))return true;
        return false;
    }
    private static String joinNatural(List<String>values){if(values.isEmpty())return "state";if(values.size()==1)return values.get(0);if(values.size()==2)return values.get(0)+" and "+values.get(1);StringBuilder b=new StringBuilder();for(int i=0;i<values.size();i++){if(i>0)b.append(i==values.size()-1?", and ":", ");b.append(values.get(i));}return b.toString();}
    private static void requirement(Set<String>s,String v){s.add(v);}
    private static void task(Set<String>s,String v){s.add(v);}
    private static void feature(String source,String refinement,Set<String>requirements,Set<String>tasks,String[]terms,String requirement,String task){if(enabled(source,refinement,terms)){requirements.add(requirement);tasks.add(task);}}
    private static void addExplicitPreference(String source,String refinement,Set<String>requirements,String term,String requirement){if(containsTerm(source,term)&&!isNegated(refinement,term))requirements.add(requirement);}
    private static String normalize(String value){return value==null?"":value.toLowerCase(Locale.US).replaceAll("\\s+"," ").trim();}
}
