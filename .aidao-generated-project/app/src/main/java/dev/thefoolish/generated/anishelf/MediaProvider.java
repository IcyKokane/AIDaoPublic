package dev.thefoolish.generated.anishelf;
import java.util.List;
public interface MediaProvider{String id();String displayName();String health();List<AnimeItem> search(String query)throws Exception;default boolean supportsPlayback(){return false;}default String resolveMediaUrl(String itemId,int episode)throws Exception{throw new UnsupportedOperationException("Provider is catalog metadata only");}}
