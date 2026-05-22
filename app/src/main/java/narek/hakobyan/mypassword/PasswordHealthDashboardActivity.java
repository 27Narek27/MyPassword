package narek.hakobyan.mypassword;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PasswordHealthDashboardActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SecureScreenUtils.apply(this);
        setContentView(R.layout.activity_password_health_dashboard);
        DatabaseHelper.PasswordHealthStats stats = new DatabaseHelper(this).getHealthStats();
        ((TextView)findViewById(R.id.tvWeak)).setText(String.valueOf(stats.weakPasswords));
        ((TextView)findViewById(R.id.tvDuplicates)).setText(String.valueOf(stats.duplicatePasswords));
        ((TextView)findViewById(R.id.tvStale)).setText(String.valueOf(stats.stalePasswords));
    }
}
