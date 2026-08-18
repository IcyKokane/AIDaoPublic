package dev.thefoolish.generated.anishelf;
import java.util.*;
public final class BuiltInProviderCatalog{private BuiltInProviderCatalog(){}public static List<MediaProvider> providers(){List<MediaProvider> out=new ArrayList<>();out.add(new JikanCatalogProvider());out.add(new AniListCatalogProvider());return out;}public static String provenance(){return "Jikan v4 (MIT) + AniList GraphQL (API terms) · catalog metadata only";}}
