package narek.hakobyan.mypassword;

import android.content.Intent;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class main_displey extends AppCompatActivity {

    RecyclerView listView;
    ArrayList<DatabaseHelper.PasswordEntry> entries;
    ArrayList<String> displayList;
    PasswordAdapter adapter;
    DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_displey);

        Button add = findViewById(R.id.btnAddPassword);
        listView = findViewById(R.id.listPasswords);

        dbHelper = new DatabaseHelper(this);

        entries = new ArrayList<>();
        displayList = new ArrayList<>();

        adapter = new PasswordAdapter(displayList, new PasswordAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(int position) {
                DatabaseHelper.PasswordEntry entry = entries.get(position);
                Intent intent = new Intent(main_displey.this, PasswordDetailActivity.class);
                intent.putExtra("id", entry.id);
                startActivity(intent);
            }
        }, position -> copyPassword(entries.get(position).password));

        listView.setLayoutManager(new LinearLayoutManager(this));
        listView.setAdapter(adapter);

        loadPasswords();

        add.setOnClickListener(v -> {
            Intent intent = new Intent(main_displey.this, dialog_password.class);
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadPasswords();
    }

    private void loadPasswords() {
        entries.clear();
        displayList.clear();

        entries.addAll(dbHelper.getAllPasswords());

        for (DatabaseHelper.PasswordEntry e : entries) {
            displayList.add(e.site + " — " + e.login);
        }

        adapter.notifyDataSetChanged();
    }

    private void copyPassword(String password) {
        ClipboardManager clipboardManager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboardManager == null) {
            Toast.makeText(this, "Clipboard unavailable", Toast.LENGTH_SHORT).show();
            return;
        }
        ClipData clipData = ClipData.newPlainText("password", password);
        clipboardManager.setPrimaryClip(clipData);
        Toast.makeText(this, "Password copied", Toast.LENGTH_SHORT).show();
    }

    // ================== ADAPTER ==================
    static class PasswordAdapter extends RecyclerView.Adapter<PasswordAdapter.PasswordViewHolder> {

        interface OnItemClickListener {
            void onItemClick(int position);
        }

        interface OnCopyClickListener {
            void onCopyClick(int position);
        }

        private ArrayList<String> items;
        private OnItemClickListener listener;
        private OnCopyClickListener copyListener;

        public PasswordAdapter(ArrayList<String> items, OnItemClickListener listener, OnCopyClickListener copyListener) {
            this.items = items;
            this.listener = listener;
            this.copyListener = copyListener;
        }

        @Override
        public PasswordViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_password, parent, false);
            return new PasswordViewHolder(view);
        }

        @Override
        public void onBindViewHolder(PasswordViewHolder holder, int position) {
            String text = items.get(position);

            String[] parts = text.split(" — ");

            holder.tvServiceName.setText(parts[0]);

            if (parts.length > 1) {
                holder.tvEmail.setText(parts[1]);
            } else {
                holder.tvEmail.setText("");
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(position);
                }
            });

            holder.btnCopyPassword.setOnClickListener(v -> {
                if (copyListener != null) {
                    copyListener.onCopyClick(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class PasswordViewHolder extends RecyclerView.ViewHolder {

            TextView tvServiceName;
            TextView tvEmail;
            Button btnCopyPassword;

            public PasswordViewHolder(View itemView) {
                super(itemView);
                tvServiceName = itemView.findViewById(R.id.tvServiceName);
                tvEmail = itemView.findViewById(R.id.tvEmail);
                btnCopyPassword = itemView.findViewById(R.id.btnCopyPassword);
            }
        }
    }
}
