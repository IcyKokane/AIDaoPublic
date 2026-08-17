package dev.thefoolish.generated.makeananimeapplikemihonit;

import android.content.Context;import android.content.SharedPreferences;
public final class LocalStore { private final SharedPreferences p; public LocalStore(Context c){p=c.getSharedPreferences("generated_app_state",Context.MODE_PRIVATE);}
 public boolean flag(String k){return p.getBoolean(k,false);} public void flag(String k,boolean v){p.edit().putBoolean(k,v).apply();}
 public int number(String k){return p.getInt(k,0);} public void number(String k,int v){p.edit().putInt(k,Math.max(0,v)).apply();}
 public String text(String k,String d){return p.getString(k,d);} public void putText(String k,String v){p.edit().putString(k,v==null?"":v).apply();} }
