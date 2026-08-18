from pathlib import Path

path = Path(__file__).resolve().parents[1] / 'android-bootstrap/app/src/main/java/dev/thefoolish/aidao/AIDaoActivityV6.java'
text = path.read_text()
old = '''        prefs=getSharedPreferences("aidao_workspace_v4",MODE_PRIVATE);
        Window w=getWindow();'''
new = '''        prefs=getSharedPreferences("aidao_workspace_v4",MODE_PRIVATE);
        recoverInterruptedBuild();
        Window w=getWindow();'''
if text.count(old) != 1:
    raise SystemExit(f'onCreate patch expected one match, found {text.count(old)}')
text = text.replace(old, new, 1)
anchor = '''    @Override protected void onDestroy(){
        cancelAuth=true;
        sessionToken="";
        super.onDestroy();
    }
'''
addition = anchor + '''
    private void recoverInterruptedBuild(){
        String stage=prefs.getString("stage","");
        if("BUILD RUNNING".equals(stage)){
            prefs.edit()
                    .putString("ci_state","Recovery required · the previous GitHub authorization/build session was interrupted. Reconnect GitHub App & Build to safely retry; session credentials were not stored.")
                    .putString("stage","BUILD RECOVERY")
                    .apply();
        }
    }
'''
if text.count(anchor) != 1:
    raise SystemExit(f'recovery method anchor expected one match, found {text.count(anchor)}')
path.write_text(text.replace(anchor, addition, 1))
print('Applied V6 interrupted-build recovery')
