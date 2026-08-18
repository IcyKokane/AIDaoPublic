from pathlib import Path

path = Path(__file__).resolve().parents[1] / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/LocalSourceGenerator.java'
text = path.read_text()
old = '''    private Domain detectDomain(String brief, List<String> requirements) {
        StringBuilder all = new StringBuilder(brief == null ? "" : brief.toLowerCase(Locale.US));
        if (requirements != null) for (String item : requirements) all.append(' ').append(item == null ? "" : item.toLowerCase(Locale.US));
        String s = all.toString();
        if (containsAny(s,"anime","episode","stream","video","media provider","playback")) return Domain.MEDIA;
        if (containsAny(s,"expense","budget","transaction","spending","finance","purchase")) return Domain.FINANCE;
        if (containsAny(s,"tracker","activity","analytics","report","habit","usage")) return Domain.TRACKER;
        if (containsAny(s,"chat","message","friend","dating","social","community")) return Domain.SOCIAL;
        if (containsAny(s,"shop","store","cart","product","checkout","order","marketplace")) return Domain.COMMERCE;
        if (containsAny(s,"note","document","article","journal","editor","write","content")) return Domain.CONTENT;
        return Domain.GENERIC;
    }

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }
'''
new = '''    private Domain detectDomain(String brief, List<String> requirements) {
        StringBuilder all = new StringBuilder(brief == null ? "" : brief.toLowerCase(Locale.US));
        if (requirements != null) for (String item : requirements) all.append(' ').append(item == null ? "" : item.toLowerCase(Locale.US));
        String s = all.toString();
        int media=score(s,"anime","episode","stream","video","media provider","playback");
        int finance=score(s,"expense","budget","transaction","spending","finance","ledger");
        int tracker=score(s,"tracker","activity tracker","analytics","habit","usage tracker","timeline");
        int social=score(s,"chat","message","friend","dating","social","community","profile","inbox");
        int commerce=score(s,"shop","store","cart","product","checkout","order","marketplace","purchase","catalog");
        int content=score(s,"note","document","article","journal","editor","write","content","draft","library search");
        int max=Math.max(media,Math.max(finance,Math.max(tracker,Math.max(social,Math.max(commerce,content)))));
        if(max==0)return Domain.GENERIC;
        if(media==max)return Domain.MEDIA;
        if(commerce==max)return Domain.COMMERCE;
        if(finance==max)return Domain.FINANCE;
        if(social==max)return Domain.SOCIAL;
        if(tracker==max)return Domain.TRACKER;
        return Domain.CONTENT;
    }

    private int score(String source,String... terms){int n=0;for(String term:terms)if(source.contains(term))n++;return n;}

    private boolean containsAny(String source, String... terms) {
        for (String term : terms) if (source.contains(term)) return true;
        return false;
    }
'''
if text.count(old) != 1:
    raise SystemExit(f'domain classifier patch expected one match, found {text.count(old)}')
path.write_text(text.replace(old,new,1))
print('Applied scored domain classifier')
