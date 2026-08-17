package dev.thefoolish.generated.makeananimeapplikemihonit;

import android.app.Activity;import android.graphics.Color;import android.os.Bundle;import android.view.Gravity;import android.widget.*;
public abstract class GeneratedScreen extends Activity { protected static class Button extends android.widget.Button { Button(android.content.Context c){super(c);} } protected LinearLayout body; protected LocalStore store;
  @Override public void onCreate(Bundle b){super.onCreate(b);store=new LocalStore(this);LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(32,48,32,32);root.setBackgroundColor(Color.rgb(18,19,24));root.addView(text("Make An Anime App Like Mihon, It",24,true));body=new LinearLayout(this);body.setOrientation(LinearLayout.VERTICAL);root.addView(body,new LinearLayout.LayoutParams(-1,-2));ScrollView s=new ScrollView(this);s.addView(root);setContentView(s);render();}
  protected abstract void render(); protected TextView text(String v,int size,boolean bold){TextView t=new TextView(this);t.setText(v);t.setTextColor(Color.WHITE);t.setTextSize(size);t.setPadding(0,10,0,10);t.setGravity(Gravity.START);t.setTypeface(android.graphics.Typeface.create("sans-serif",bold?1:0));return t;}
  protected Button action(String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);b.setMinHeight(52);return b;} protected void gap(){Space s=new Space(this);body.addView(s,new LinearLayout.LayoutParams(1,16));}
}
