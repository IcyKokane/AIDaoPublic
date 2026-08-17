package dev.thefoolish.generated.makeananimeapplikemihonit;

import android.app.Activity;import android.content.Intent;
public final class AppNavigator { private AppNavigator(){} public static void open(Activity a,Class<? extends Activity> target){a.startActivity(new Intent(a,target));} }
