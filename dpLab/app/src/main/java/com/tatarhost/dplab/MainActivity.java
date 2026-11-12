package com.tatarhost.dplab;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_FOLDER = 20;
    private static final int PICK_FILE = 21;
    private static final int BG = Color.rgb(9, 12, 16);
    private static final int PANEL = Color.rgb(20, 25, 32);
    private static final int PANEL2 = Color.rgb(28, 34, 43);
    private static final int FG = Color.rgb(242, 245, 248);
    private static final int MUTED = Color.rgb(156, 166, 180);
    private static final int ACCENT = Color.rgb(216, 255, 98);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Skill> skills = new ArrayList<>();
    private SharedPreferences prefs;
    private String page = "ai";
    private String draft = "";
    private String activeSkill = "general";
    private EditText prompt;
    private EditText tokenField;
    private TextView githubState;
    private TextView repoList;

    private static final class Skill {
        final String id, name, instruction;
        final boolean custom;
        Skill(String id, String name, String instruction, boolean custom) {
            this.id = id; this.name = name; this.instruction = instruction; this.custom = custom;
        }
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("dplab", MODE_PRIVATE);
        activeSkill = prefs.getString("active_skill", "general");
        loadSkills();
        render();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(42, 50, 61));
        return d;
    }

    private LinearLayout column() {
        LinearLayout v = new LinearLayout(this);
        v.setOrientation(LinearLayout.VERTICAL);
        return v;
    }

    private TextView text(String value, int size, int color, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(color);
        if (bold) v.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return v;
    }

    private void add(LinearLayout p, View v, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        p.addView(v, lp);
    }

    private TextView button(String title, Runnable click) {
        TextView v = text(title, 14, FG, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(14), dp(13), dp(14), dp(13));
        v.setBackground(bg(PANEL2, 14));
        v.setOnClickListener(w -> click.run());
        return v;
    }

    private EditText edit(String hint, int lines, boolean secret) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextColor(FG);
        e.setHintTextColor(MUTED);
        e.setTextSize(15);
        e.setPadding(dp(13), dp(11), dp(13), dp(11));
        e.setMinLines(lines);
        e.setGravity(lines > 1 ? Gravity.TOP : Gravity.CENTER_VERTICAL);
        e.setBackground(bg(PANEL2, 13));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    private LinearLayout card(String title, String note) {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(bg(PANEL, 18));
        if (title != null) add(c, text(title, 17, FG, true), 0);
        if (note != null) add(c, text(note, 12, MUTED, false), 7);
        return c;
    }

    private void render() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(16), dp(14), dp(16), dp(10));
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout names = column();
        names.addView(text("dpLab", 28, FG, true));
        names.addView(text("DeepSeek companion workspace", 12, MUTED, false));
        head.addView(names, new LinearLayout.LayoutParams(0, -2, 1));
        TextView local = text("● LOCAL", 10, ACCENT, true);
        local.setPadding(dp(10), dp(7), dp(10), dp(7));
        local.setBackground(bg(PANEL, 18));
        head.addView(local);
        root.addView(head);

        HorizontalScrollView bar = new HorizontalScrollView(this);
        bar.setHorizontalScrollBarEnabled(false);
        LinearLayout tabs = new LinearLayout(this);
        String[][] all = {{"ai","AI"},{"files","Файлы"},{"connectors","Коннекторы"},{"skills","Скилы"}};
        for (String[] item : all) {
            TextView tab = text(item[1], 13, page.equals(item[0]) ? BG : MUTED, true);
            tab.setGravity(Gravity.CENTER);
            tab.setPadding(dp(16), dp(11), dp(16), dp(11));
            tab.setBackground(bg(page.equals(item[0]) ? ACCENT : PANEL, 13));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(7);
            tabs.addView(tab, lp);
            tab.setOnClickListener(v -> {
                if (prompt != null) draft = prompt.getText().toString();
                page = item[0];
                render();
            });
        }
        bar.addView(tabs);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(-1, -2);
        bp.topMargin = dp(17); bp.bottomMargin = dp(13);
        root.addView(bar, bp);

        ScrollView scroll = new ScrollView(this);
        LinearLayout body = column();
        body.setPadding(0, 0, 0, dp(22));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        if (page.equals("files")) filesPage(body);
        else if (page.equals("connectors")) connectorsPage(body);
        else if (page.equals("skills")) skillsPage(body);
        else aiPage(body);
    }

    private void aiPage(LinearLayout body) {
        add(body, text("Рабочее пространство", 22, FG, true), 2);
        add(body, text("Подготовь запрос, выбери навык и передай текст в DeepSeek.", 13, MUTED, false), 7);
        LinearLayout c = card("Сообщение", "Текст остаётся на устройстве, пока ты сам его не отправишь.");
        prompt = edit("Напиши задачу для DeepSeek…", 6, false);
        prompt.setText(draft);
        add(c, prompt, 12);
        add(c, text("Активный навык: " + selectedSkill().name, 12, ACCENT, true), 10);
        add(c, button("Выбрать навык", this::chooseSkill), 9);
        add(c, button("Подготовить и отправить в DeepSeek", () -> {
            draft = prompt.getText().toString();
            if (draft.trim().isEmpty()) { toast("Сначала напиши запрос"); return; }
            String value = selectedSkill().instruction + "\n\nЗадача:\n" + draft;
            Intent send = new Intent(Intent.ACTION_SEND);
            send.setType("text/plain");
            send.putExtra(Intent.EXTRA_TEXT, value);
            startActivity(Intent.createChooser(send, "Передать запрос в приложение"));
        }), 9);
        add(c, button("Скопировать запрос", () -> {
            draft = prompt.getText().toString();
            copy(selectedSkill().instruction + "\n\nЗадача:\n" + draft);
            toast("Запрос скопирован");
        }), 9);
        add(body, c, 14);
        LinearLayout deepseek = card("DeepSeek", "dpLab не заменяет и не перепаковывает оригинальный APK.");
        add(deepseek, button("Открыть DeepSeek", () -> open("https://chat.deepseek.com/")), 10);
        add(deepseek, text("Для передачи текста выбери DeepSeek в системном меню «Поделиться», если приложение поддерживает этот тип входящих данных.", 12, MUTED, false), 9);
        add(body, deepseek, 12);
    }

    private void filesPage(LinearLayout body) {
        add(body, text("Файлы устройства", 22, FG, true), 2);
        add(body, text("Только папки и файлы, которые ты сам выбираешь в системном окне Android.", 13, MUTED, false), 7);
        LinearLayout c = card("Папка для сохранения", "Разрешение на выбранную папку сохраняется для dpLab.");
        boolean selected = prefs.getString("folder_uri", null) != null;
        add(c, text(selected ? "Папка подключена ✓" : "Папка не выбрана", 13, selected ? ACCENT : MUTED, true), 9);
        add(c, button("Выбрать папку", this::pickFolder), 9);
        add(c, button("Создать текстовый файл", () -> createFile("dpLab-note.txt", draft)), 9);
        add(c, button("Открыть текстовый файл", this::pickFile), 9);
        add(body, c, 13);
        LinearLayout info = card("Локальная работа", "Создавай заметки, Markdown и JSON-файлы. Для чтения выбери документ вручную.");
        add(info, text("Полный доступ ко всему хранилищу не запрашивается.", 12, MUTED, false), 8);
        add(body, info, 12);
    }

    private void connectorsPage(LinearLayout body) {
        add(body, text("Подключения", 22, FG, true), 2);
        add(body, text("Секреты не отправляются на dpLab-сервер. GitHub-токен шифруется Android Keystore.", 13, MUTED, false), 7);
        LinearLayout gh = card("GitHub", "Список репозиториев и создание приватных Gist через GitHub API.");
        tokenField = edit("GitHub Personal Access Token", 1, true);
        add(gh, tokenField, 11);
        add(gh, button("Зашифрованно сохранить токен", this::saveToken), 9);
        add(gh, button("Проверить подключение и загрузить репозитории", this::checkGithub), 9);
        add(gh, button("Создать приватный Gist", this::gistDialog), 9);
        add(gh, button("Удалить сохранённый токен", this::clearToken), 9);
        githubState = text("GitHub ещё не проверен", 12, MUTED, false);
        add(gh, githubState, 11);
        repoList = text("", 12, FG, false);
        add(gh, repoList, 8);
        add(body, gh, 13);
        LinearLayout local = card("Файлы устройства", "Выбранная папка, создание файлов и чтение выбранных документов.");
        add(local, button("Открыть файловый раздел", () -> { page = "files"; render(); }), 9);
        add(body, local, 12);
        LinearLayout scopes = card("Разрешения GitHub", "Для репозиториев: Metadata read-only. Для приватных Gist: Gists read/write. Не вставляй токен в чат и не коммить его в репозиторий.");
        add(body, scopes, 12);
    }

    private void skillsPage(LinearLayout body) {
        add(body, text("Навыки", 22, FG, true), 2);
        add(body, text("Локальные шаблоны инструкций, добавляемые к сообщению перед передачей в DeepSeek.", 13, MUTED, false), 7);
        add(body, button("+ Создать свой навык", this::newSkillDialog), 12);
        for (Skill skill : skills) {
            LinearLayout c = card(skill.name, (skill.id.equals(activeSkill) ? "● Активен · " : "")
                    + (skill.custom ? "Пользовательский" : "Встроенный шаблон"));
            add(c, text(skill.instruction, 12, MUTED, false), 8);
            add(c, button(skill.id.equals(activeSkill) ? "Выбран" : "Использовать", () -> {
                activeSkill = skill.id;
                prefs.edit().putString("active_skill", activeSkill).apply();
                render();
            }), 9);
            if (skill.custom) add(c, button("Удалить", () -> {
                skills.remove(skill);
                saveSkills();
                if (activeSkill.equals(skill.id)) activeSkill = "general";
                prefs.edit().putString("active_skill", activeSkill).apply();
                render();
            }), 8);
            add(body, c, 10);
        }
    }

    private void loadSkills() {
        skills.clear();
        skills.add(new Skill("general", "Универсальный помощник",
                "Отвечай понятно и по делу. Отделяй факты от предположений.", false));
        skills.add(new Skill("code", "Разработка",
                "Действуй как опытный разработчик. Предлагай рабочий код и учитывай безопасность.", false));
        skills.add(new Skill("research", "Исследование",
                "Составь план исследования, используй проверяемые источники и отмечай, что требует актуальной проверки.", false));
        skills.add(new Skill("writing", "Документы и текст",
                "Подготовь аккуратный структурированный текст. Не выдумывай факты.", false));
        try {
            JSONArray arr = new JSONArray(prefs.getString("custom_skills", "[]"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                skills.add(new Skill(o.optString("id"), o.optString("name"),
                        o.optString("instruction"), true));
            }
        } catch (Exception ignored) { prefs.edit().putString("custom_skills", "[]").apply(); }
    }

    private Skill selectedSkill() {
        for (Skill s : skills) if (s.id.equals(activeSkill)) return s;
        return skills.get(0);
    }

    private void saveSkills() {
        JSONArray arr = new JSONArray();
        try {
            for (Skill s : skills) if (s.custom) {
                JSONObject o = new JSONObject();
                o.put("id", s.id); o.put("name", s.name); o.put("instruction", s.instruction);
                arr.put(o);
            }
            prefs.edit().putString("custom_skills", arr.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void chooseSkill() {
        String[] names = new String[skills.size()];
        for (int i = 0; i < skills.size(); i++) names[i] = skills.get(i).name;
        new AlertDialog.Builder(this).setTitle("Выбрать навык").setItems(names, (d, which) -> {
            activeSkill = skills.get(which).id;
            prefs.edit().putString("active_skill", activeSkill).apply();
            render();
        }).setNegativeButton("Отмена", null).show();
    }

    private void newSkillDialog() {
        LinearLayout form = column();
        form.setPadding(dp(16), dp(8), dp(16), 0);
        EditText name = edit("Название навыка", 1, false);
        EditText instructions = edit("Инструкции для DeepSeek", 4, false);
        add(form, name, 0); add(form, instructions, 9);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Новый навык")
                .setView(form).setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
            String n = name.getText().toString().trim();
            String t = instructions.getText().toString().trim();
            if (n.isEmpty() || t.isEmpty()) { toast("Заполни оба поля"); return; }
            activeSkill = "custom-" + System.currentTimeMillis();
            skills.add(new Skill(activeSkill, n, t, true));
            saveSkills();
            prefs.edit().putString("active_skill", activeSkill).apply();
            dialog.dismiss(); render();
        }));
        dialog.show();
    }

    private void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_FOLDER);
    }

    private void pickFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, PICK_FILE);
    }

    private void createFile(String defaultName, String initialText) {
        String treeValue = prefs.getString("folder_uri", null);
        if (treeValue == null) { toast("Сначала выбери папку"); pickFolder(); return; }
        LinearLayout form = column();
        form.setPadding(dp(16), dp(8), dp(16), 0);
        EditText filename = edit("Имя файла", 1, false);
        filename.setText(defaultName);
        EditText content = edit("Содержимое", 7, false);
        content.setText(initialText == null ? "" : initialText);
        add(form, filename, 0); add(form, content, 9);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Создать файл")
                .setView(form).setNegativeButton("Отмена", null)
                .setPositiveButton("Создать", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
            String name = filename.getText().toString().trim();
            if (name.isEmpty() || name.contains("/") || name.contains("\\")) {
                toast("Некорректное имя файла"); return;
            }
            try {
                Uri tree = Uri.parse(treeValue);
                Uri parent = DocumentsContract.buildDocumentUriUsingTree(
                        tree, DocumentsContract.getTreeDocumentId(tree));
                Uri file = DocumentsContract.createDocument(getContentResolver(), parent, "text/plain", name);
                if (file == null) throw new IllegalStateException("Android не создал файл");
                try (OutputStream out = getContentResolver().openOutputStream(file, "w")) {
                    if (out == null) throw new IllegalStateException("Не удалось открыть файл");
                    out.write(content.getText().toString().getBytes(StandardCharsets.UTF_8));
                }
                dialog.dismiss(); toast("Создан файл: " + name);
            } catch (Exception e) { toast("Ошибка создания: " + shortError(e)); }
        }));
        dialog.show();
    }

    private void readFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalStateException("Не удалось открыть документ");
            byte[] b = new byte[4096]; int n;
            while ((n = in.read(b)) != -1) {
                if (out.size() + n > 1024 * 1024) throw new IllegalStateException("Файл больше 1 МБ");
                out.write(b, 0, n);
            }
            String value = out.toString(StandardCharsets.UTF_8.name());
            EditText preview = edit("Содержимое файла", 12, false);
            preview.setText(value); preview.setSelection(0);
            new AlertDialog.Builder(this).setTitle("Текстовый файл").setView(preview)
                    .setPositiveButton("Скопировать", (d, w) -> copy(value))
                    .setNegativeButton("Закрыть", null).show();
        } catch (Exception e) { toast("Не удалось прочитать: " + shortError(e)); }
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == PICK_FOLDER) {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(uri, flags);
                prefs.edit().putString("folder_uri", uri.toString()).apply();
                toast("Папка подключена");
            } catch (Exception e) { toast("Не удалось сохранить доступ: " + shortError(e)); }
            page = "files"; render();
        } else if (request == PICK_FILE) readFile(uri);
    }

    private void saveToken() {
        String token = tokenField == null ? "" : tokenField.getText().toString().trim();
        if (token.isEmpty()) { toast("Вставь GitHub token"); return; }
        try {
            SecureTokenStore.save(this, token);
            tokenField.setText("");
            if (githubState != null) githubState.setText("Токен сохранён в Android Keystore");
            toast("Токен зашифрован и сохранён");
        } catch (Exception e) { toast("Ошибка сохранения: " + shortError(e)); }
    }

    private String token() {
        try { return SecureTokenStore.load(this); }
        catch (Exception e) { toast("Ошибка чтения токена: " + shortError(e)); return null; }
    }

    private void clearToken() {
        SecureTokenStore.clear(this);
        if (githubState != null) githubState.setText("Токен удалён");
        if (repoList != null) repoList.setText("");
        toast("GitHub token удалён");
    }

    private void checkGithub() {
        String value = token();
        if (value == null || value.isEmpty()) { toast("Сначала сохрани токен"); return; }
        if (githubState != null) githubState.setText("Подключение…");
        executor.execute(() -> {
            try {
                JSONObject user = GitHubClient.user(value);
                List<String> repos = GitHubClient.repositories(value);
                StringBuilder result = new StringBuilder("Репозитории:\n");
                for (String item : repos) result.append("• ").append(item).append('\n');
                if (repos.isEmpty()) result.append("Нет доступных репозиториев");
                String login = user.optString("login", "GitHub");
                runOnUiThread(() -> {
                    if (githubState != null) githubState.setText("Подключено: @" + login);
                    if (repoList != null) repoList.setText(result.toString());
                });
            } catch (Exception e) {
                String error = shortError(e);
                runOnUiThread(() -> { if (githubState != null) githubState.setText("Ошибка: " + error); });
            }
        });
    }

    private void gistDialog() {
        String savedToken = token();
        if (savedToken == null || savedToken.isEmpty()) { toast("Сначала подключи GitHub"); return; }
        LinearLayout form = column();
        form.setPadding(dp(16), dp(8), dp(16), 0);
        EditText filename = edit("Имя файла", 1, false); filename.setText("note.md");
        EditText description = edit("Описание", 1, false);
        EditText content = edit("Содержимое Gist", 5, false);
        add(form, filename, 0); add(form, description, 8); add(form, content, 8);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Приватный Gist")
                .setView(form).setNegativeButton("Отмена", null)
                .setPositiveButton("Создать", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
            String name = filename.getText().toString().trim();
            String desc = description.getText().toString().trim();
            String body = content.getText().toString();
            if (name.isEmpty() || name.contains("/") || body.isEmpty()) { toast("Заполни имя и содержимое"); return; }
            dialog.dismiss(); toast("Создаю Gist…");
            executor.execute(() -> {
                try {
                    String url = GitHubClient.createPrivateGist(savedToken, name, desc, body);
                    runOnUiThread(() -> { toast("Приватный Gist создан"); if (!url.isEmpty()) open(url); });
                } catch (Exception e) {
                    String error = shortError(e);
                    runOnUiThread(() -> toast("Ошибка Gist: " + error));
                }
            });
        }));
        dialog.show();
    }

    private void copy(String value) {
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        manager.setPrimaryClip(ClipData.newPlainText("dpLab", value));
    }

    private void open(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast("Не удалось открыть ссылку"); }
    }

    private String shortError(Exception e) {
        String m = e.getMessage();
        if (m == null || m.trim().isEmpty()) m = e.getClass().getSimpleName();
        return m.length() > 130 ? m.substring(0, 130) : m;
    }

    private void toast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
