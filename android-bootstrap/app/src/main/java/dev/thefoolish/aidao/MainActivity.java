package dev.thefoolish.aidao;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Color;
import android.view.View;
import android.widget.*;
import android.text.InputType;

public class MainActivity extends Activity {
    private LinearLayout root;
    private TextView output;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(32, 48, 32, 32);
        root.setBackgroundColor(Color.rgb(28, 30, 36));

        TextView title = text("AIDao", 28, true);
        TextView subtitle = text("Describe an Android app in ordinary language.", 16, false);
        EditText request = new EditText(this);
        request.setHint("Example: Build an anime library app with provider plugins...");
        request.setTextColor(Color.WHITE);
        request.setHintTextColor(Color.LTGRAY);
        request.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        request.setMinLines(5);

        Button plan = new Button(this);
        plan.setText("CREATE PROJECT BRIEF");
        output = text("Alpha shell ready. Local planning is available; GitHub/OAuth and model-backed generation are being integrated.", 15, false);

        plan.setOnClickListener(v -> {
            String raw = request.getText().toString().trim();
            if (raw.isEmpty()) {
                output.setText("Enter a description first.");
                return;
            }
            output.setText("Project brief\n\nGoal: " + raw + "\n\nAIDao will expand this into requirements, implementation tasks, validation, build, repair, and APK delivery steps.");
        });

        root.addView(title);
        root.addView(subtitle);
        root.addView(request, new LinearLayout.LayoutParams(-1, -2));
        root.addView(plan, new LinearLayout.LayoutParams(-1, -2));
        root.addView(output, new LinearLayout.LayoutParams(-1, -2));
        setContentView(root);
    }

    private TextView text(String s, int sp, boolean bold) {
        TextView v = new TextView(this);
        v.setText(s);
        v.setTextColor(Color.WHITE);
        v.setTextSize(sp);
        v.setPadding(0, 10, 0, 20);
        if (bold) v.setTypeface(null, android.graphics.Typeface.BOLD);
        return v;
    }
}
