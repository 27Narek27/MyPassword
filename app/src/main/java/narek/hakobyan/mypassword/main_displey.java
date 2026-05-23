package narek.hakobyan.mypassword;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class main_displey extends AppCompatActivity {

    RecyclerView listView;
    ArrayList<DatabaseHelper.PasswordEntry> allEntries    = new ArrayList<>();
    ArrayList<DatabaseHelper.PasswordEntry> filteredEntries = new ArrayList<>();
    PasswordAdapter adapter;
    DatabaseHelper dbHelper;
    EditText etSearch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SecureScreenUtils.apply(this);
        setContentView(R.layout.activity_main_displey);

        Button add   = findViewById(R.id.btnAddPassword);
        Button dashboard = findViewById(R.id.btnHealthDashboard);
        listView     = findViewById(R.id.listPasswords);
        etSearch     = findViewById(R.id.etSearch);

        dbHelper = new DatabaseHelper(this);

        ensureHoneytokens();

        adapter = new PasswordAdapter(filteredEntries,
                position -> {
                    DatabaseHelper.PasswordEntry entry = filteredEntries.get(position);
                    Intent intent = new Intent(main_displey.this, PasswordDetailActivity.class);
                    intent.putExtra("id", entry.id);
                    startActivity(intent);
                },
                position -> copyPassword(filteredEntries.get(position).password));

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);
        enableSwipeToDelete();

        loadPasswords();

        add.setOnClickListener(v ->
                startActivity(new Intent(main_displey.this, dialog_password.class)));
        dashboard.setOnClickListener(v ->
                startActivity(new Intent(main_displey.this, PasswordHealthDashboardActivity.class)));

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int st, int c, int a) {}
            @Override public void onTextChanged(CharSequence s, int st, int b, int c) { filterList(s.toString()); }
            @Override public void afterTextChanged(Editable s) {}
        });
    }


    private void enableSwipeToDelete() {
        ItemTouchHelper.SimpleCallback callback = new ItemTouchHelper.SimpleCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getBindingAdapterPosition();
                if (position < 0 || position >= filteredEntries.size()) {
                    adapter.notifyDataSetChanged();
                    return;
                }
                DatabaseHelper.PasswordEntry entry = filteredEntries.get(position);
                dbHelper.deletePassword(entry.id);
                Toast.makeText(main_displey.this, "Пароль удалён", Toast.LENGTH_SHORT).show();
                loadPasswords();
            }
        };
        new ItemTouchHelper(callback).attachToRecyclerView(listView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPasswords();
    }


    private void ensureHoneytokens() {
        android.content.SharedPreferences prefs = getSharedPreferences("stealth", MODE_PRIVATE);
        if (!prefs.getBoolean("honeytoken_seeded", false)) {
            dbHelper.insertHoneytokenData();
            prefs.edit().putBoolean("honeytoken_seeded", true).apply();
        }
    }

    private void loadPasswords() {
        allEntries.clear();
        allEntries.addAll(dbHelper.getVisiblePasswords());
        filterList(etSearch != null ? etSearch.getText().toString() : "");
    }

    private void filterList(String query) {
        filteredEntries.clear();
        String q = query.trim().toLowerCase();
        if (q.isEmpty()) {
            filteredEntries.addAll(allEntries);
        } else {
            for (DatabaseHelper.PasswordEntry e : allEntries) {
                if (e.site.toLowerCase().contains(q)
                        || e.login.toLowerCase().contains(q)
                        || (e.websiteUrl != null && e.websiteUrl.toLowerCase().contains(q))) {
                    filteredEntries.add(e);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void copyPassword(String password) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (cm == null) {
            Toast.makeText(this, "Буфер обмена недоступен", Toast.LENGTH_SHORT).show();
            return;
        }
        cm.setPrimaryClip(ClipData.newPlainText("password", password));
        Toast.makeText(this, "Пароль скопирован", Toast.LENGTH_SHORT).show();
    }

    /* ──────────────────────────────────────────────────────────────────
       Adapter
       ────────────────────────────────────────────────────────────────── */

    static class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.VH> {

        interface OnItemClick  { void onClick(int position); }
        interface OnCopyClick  { void onCopy(int position);  }

        private final ArrayList<DatabaseHelper.PasswordEntry> items;
        private final OnItemClick  itemListener;
        private final OnCopyClick  copyListener;

        PasswordAdapter(ArrayList<DatabaseHelper.PasswordEntry> items,
                        OnItemClick itemListener, OnCopyClick copyListener) {
            this.items        = items;
            this.itemListener = itemListener;
            this.copyListener = copyListener;
        }

        @Override
        public VH onCreateViewHolder(ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_password, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(VH h, int pos) {
            DatabaseHelper.PasswordEntry e = items.get(pos);
            h.tvServiceName.setText((e.isFavorite ? "★ " : "") + (!e.site.isEmpty() ? e.site : "(без названия)"));
            h.tvEmail.setText((e.login != null ? e.login : "") + " • " + (e.category != null ? e.category : "Общее"));
            h.itemView.setOnClickListener(v -> { if (itemListener != null) itemListener.onClick(pos); });
            h.btnCopy.setOnClickListener(v -> { if (copyListener != null) copyListener.onCopy(pos); });
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView tvServiceName, tvEmail;
            Button btnCopy;
            VH(View v) {
                super(v);
                tvServiceName = v.findViewById(R.id.tvServiceName);
                tvEmail       = v.findViewById(R.id.tvEmail);
                btnCopy       = v.findViewById(R.id.btnCopyPassword);
            }
        }
    }
}