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

        int[] opIds = {R.id.btnPlus, R.id.btnMinus, R.id.btnMul, R.id.btnDiv};
        for (int id : opIds) {
            Button b = findViewById(id);
            b.setOnClickListener(v -> append(((Button) v).getText().toString()));
        }

        findViewById(R.id.btnClear).setOnClickListener(v -> { input.setLength(0); tvDisplay.setText("0"); });
        findViewById(R.id.btnEq).setOnClickListener(v -> evaluate());
    }

    private void append(String token) {
        input.append(token);
        tvDisplay.setText(input.toString());
    }

    private void evaluate() {
        String expr = input.toString().replace("×", "*").replace("÷", "/").replace(" ", "");
        if ("27+27".equals(expr)) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }
        try {
            double value = simpleEval(expr);
            String out = (value == (long) value) ? String.valueOf((long) value) : String.valueOf(value);
            tvDisplay.setText(out);
            input.setLength(0);
            input.append(out);
        } catch (Exception e) {
            tvDisplay.setText("Error");
            input.setLength(0);
        }
    }

    private double simpleEval(String expr) {
        if (expr.isEmpty()) throw new IllegalArgumentException("empty");
        double current;
        int i = 0;
        int n = expr.length();
        StringBuilder num = new StringBuilder();
        while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) num.append(expr.charAt(i++));
        current = Double.parseDouble(num.toString());
        while (i < n) {
            char op = expr.charAt(i++);
            num.setLength(0);
            while (i < n && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) num.append(expr.charAt(i++));
            double right = Double.parseDouble(num.toString());
            if (op == '+') current += right;
            else if (op == '-') current -= right;
            else if (op == '*') current *= right;
            else if (op == '/') current /= right;
            else throw new IllegalArgumentException("bad op");
        }
        return current;
    }
}
