package dev.thefoolish.aidao;

import java.util.Locale;

/**
 * Immutable, non-executable representation of material explicitly shared with AIDao.
 * This foundation deliberately treats imported material as knowledge/data only.
 */
final class KnowledgeDocument {
    final String displayName;
    final String mimeType;
    final String sourceLabel;
    final String text;
    final boolean executableLike;

    private KnowledgeDocument(String displayName,String mimeType,String sourceLabel,String text,boolean executableLike){
        this.displayName=displayName;
        this.mimeType=mimeType;
        this.sourceLabel=sourceLabel;
        this.text=text;
        this.executableLike=executableLike;
    }

    static KnowledgeDocument fromUserSharedText(String displayName,String mimeType,String sourceLabel,String raw){
        String name=clean(displayName,"Shared material");
        String mime=clean(mimeType,"text/plain");
        String source=clean(sourceLabel,"User shared");
        String value=raw==null?"":raw.replace("\u0000","").trim();
        if(value.length()>200_000)value=value.substring(0,200_000);
        String lower=(name+" "+mime).toLowerCase(Locale.US);
        boolean executable=lower.endsWith(".apk")||lower.endsWith(".exe")||lower.endsWith(".msi")||lower.endsWith(".bat")||lower.endsWith(".cmd")||lower.endsWith(".sh")||lower.contains("application/vnd.android.package-archive")||lower.contains("application/x-msdownload");
        return new KnowledgeDocument(name,mime,source,value,executable);
    }

    boolean mayUseAsPlanningKnowledge(){return !executableLike&&!text.isEmpty();}

    String boundedPlanningExcerpt(){
        if(!mayUseAsPlanningKnowledge())return "";
        int max=Math.min(text.length(),12_000);
        return text.substring(0,max);
    }

    private static String clean(String value,String fallback){
        if(value==null||value.trim().isEmpty())return fallback;
        String v=value.replaceAll("[\\r\\n\\t]+"," ").trim();
        return v.length()>160?v.substring(0,160):v;
    }
}
