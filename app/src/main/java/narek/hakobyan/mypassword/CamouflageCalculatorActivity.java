package narek.hakobyan.mypassword;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class CamouflageCalculatorActivity extends AppCompatActivity {

    private final StringBuilder input = new StringBuilder();
    private TextView tvDisplay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camouflage_calculator);

        tvDisplay = findViewById(R.id.tvCalcDisplay);

        int[] digitIds = {R.id.btn0, R.id.btn1, R.id.btn2, R.id.btn3, R.id.btn4, R.id.btn5, R.id.btn6, R.id.btn7, R.id.btn8, R.id.btn9};
        for (int id : digitIds) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> append(((Button) v).getText().toString()));
        }

        findViewById(R.id.btnClear).setOnClickListener(v -> { input.setLength(0); tvDisplay.setText("0"); });
        findViewById(R.id.btnEq).setOnClickListener(v -> tvDisplay.setText(input.length() == 0 ? "0" : input.toString()));
    }

    private void append(String n) {
        input.append(n);
        tvDisplay.setText(input.toString());
        if (input.toString().endsWith("9870")) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }
}
