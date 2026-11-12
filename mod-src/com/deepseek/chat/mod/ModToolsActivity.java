package com.deepseek.chat.mod;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
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

public final class ModToolsActivity extends Activity {
    private static final int PICK_FOLDER = 71;
    private static final int PICK_FILE = 72;
    private static final int BG = Color.rgb(9, 12, 16);
    private static final int PANEL = Color.rgb(20, 25, 32);
    private static final int PANEL_2 = Color.rgb(29, 36, 45);
    private static final int FG = Color.rgb(242, 245, 248);
    private static final int MUTED = Color.rgb(153, 164, 179);
    private static final int ACCENT = Color.rgb(216, 255, 98);

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final List<Skill> skills = new ArrayList<>();
    private SharedPreferences prefs;
    private LinearLayout body;
    private EditText promptField;
    private EditText tokenField;
    private TextView githubStatus;
    private TextView reposList;
    private String tab = "ai";
    private String promptDraft = "";
    private String activeSkill = "general";

    private static final class Skill {
        final String id;
        final String name;
        final String instruction;
        final boolean custom;
        Skill(String id, String name, String instruction, boolean custom) {
            this.id = id; this.name = name; this.instruction = instruction; this.custom = custom;
        }
    }

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("dplab_mod", MODE_PRIVATE);
        activeSkill = prefs.getString("active_skill", "general");
        loadSkills();
        getWindow().setStatusBarColor(BG);
        getWindow().setNavigationBarColor(BG);
        render();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable shape(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(dp(radius));
        d.setStroke(dp(1), Color.rgb(43, 52, 64));
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

    private void add(LinearLayout parent, View child, int top) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(top);
        parent.addView(child, lp);
    }

    private TextView button(String value, Runnable onClick) {
        TextView v = text(value, 14, FG, true);
        v.setGravity(Gravity.CENTER);
        v.setPadding(dp(13), dp(12), dp(13), dp(12));
        v.setBackground(shape(PANEL_2, 14));
        v.setOnClickListener(view -> onClick.run());
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
        e.setBackground(shape(PANEL_2, 12));
        if (secret) e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        return e;
    }

    private LinearLayout card(String title, String description) {
        LinearLayout c = column();
        c.setPadding(dp(16), dp(16), dp(16), dp(16));
        c.setBackground(shape(PANEL, 18));
        add(c, text(title, 17, FG, true), 0);
        if (description != null && !description.isEmpty()) {
            add(c, text(description, 12, MUTED, false), 6);
        }
        return c;
    }

    private void render() {
        LinearLayout root = column();
        root.setBackgroundColor(BG);
        root.setPadding(dp(15), dp(13), dp(15), dp(10));

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout labels = column();
        labels.addView(text("dpLab", 27, FG, true));
        labels.addView(text("DeepSeek mod tools", 12, MUTED, false));
        header.addView(labels, new LinearLayout.LayoutParams(0, -2, 1));
        TextView back = button("×", this::finish);
        header.addView(back, new LinearLayout.LayoutParams(dp(46), dp(42)));
        root.addView(header);

        HorizontalScrollView navScroll = new HorizontalScrollView(this);
        navScroll.setHorizontalScrollBarEnabled(false);
        LinearLayout nav = new LinearLayout(this);
        String[][] tabs = {{"ai","AI"},{"files","Файлы"},{"github","GitHub"},{"skills","Навыки"}};
        for (String[] item : tabs) {
            TextView b = text(item[1], 13, tab.equals(item[0]) ? BG : MUTED, true);
            b.setGravity(Gravity.CENTER);
            b.setPadding(dp(16), dp(11), dp(16), dp(11));
            b.setBackground(shape(tab.equals(item[0]) ? ACCENT : PANEL, 13));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.rightMargin = dp(7);
            nav.addView(b, lp);
            b.setOnClickListener(view -> {
                saveDraft();
                tab = item[0];
                render();
            });
        }
        navScroll.addView(nav);
        LinearLayout.LayoutParams navLp = new LinearLayout.LayoutParams(-1, -2);
        navLp.topMargin = dp(16);
        navLp.bottomMargin = dp(12);
        root.addView(navScroll, navLp);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        body = column();
        body.setPadding(0, 0, 0, dp(20));
        scroll.addView(body);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root);

        if (tab.equals("files")) filesPage();
        else if (tab.equals("github")) githubPage();
        else if (tab.equals("skills")) skillsPage();
        else aiPage();
    }

    private void saveDraft() {
        if (promptField != null) promptDraft = promptField.getText().toString();
        promptField = null;
    }

    private void aiPage() {
        add(body, text("Инструменты для родного чата", 21, FG, true), 2);
        add(body, text("DeepSeek остаётся оригинальным приложением. Здесь — дополнительные действия и навыки.", 13, MUTED, false), 6);

        LinearLayout c = card("Подготовить запрос", "Текст можно передать в штатный обработчик DeepSeek.");
        promptField = edit("Задача для DeepSeek…", 6, false);
        promptField.setText(promptDraft);
        add(c, promptField, 12);
        add(c, text("Навык: " + selectedSkill().name, 12, ACCENT, true), 10);
        add(c, button("Выбрать навык", this::chooseSkill), 8);
        add(c, button("Отправить в оригинальный DeepSeek", this::sendToDeepSeek), 8);
        add(c, button("Скопировать запрос", () -> {
            saveDraft();
            copy(buildPrompt(promptDraft));
            toast("Запрос скопирован");
        }), 8);
        add(body, c, 12);

        LinearLayout shortcuts = card("Быстрый доступ", "Закрой панель, чтобы вернуться к открытому чату.");
        add(shortcuts, button("Вернуться в DeepSeek", this::finish), 8);
        add(shortcuts, button("Сохранить текущий текст как файл", () -> {
            saveDraft();
            createFileDialog("deepseek-prompt.txt", buildPrompt(promptDraft));
        }), 8);
        add(body, shortcuts, 12);
    }

    private void sendToDeepSeek() {
        saveDraft();
        if (promptDraft.trim().isEmpty()) {
            toast("Сначала напиши запрос");
            return;
        }
        try {
            Intent share = new Intent(Intent.ACTION_SEND);
            share.setType("text/plain");
            share.putExtra(Intent.EXTRA_TEXT, buildPrompt(promptDraft));
            share.setComponent(new ComponentName(
                    "com.deepseek.chat", "com.deepseek.chat.ShareProxyActivity"));
            startActivity(share);
            finish();
        } catch (Exception e) {
            copy(buildPrompt(promptDraft));
            toast("Не удалось передать запрос автоматически. Текст скопирован.");
        }
    }

    private String buildPrompt(String task) {
        return selectedSkill().instruction + "\n\nЗадача:\n" + task;
    }

    private void filesPage() {
        add(body, text("Файлы на устройстве", 21, FG, true), 2);
        add(body, text("Используется системный выбор папок Android. dpLab не получает общий доступ к хранилищу.", 13, MUTED, false), 7);
        String stored = prefs.getString("folder_uri", null);
        LinearLayout c = card("Папка", stored == null ? "Папка не выбрана" : "Папка подключена ✓");
        add(c, button("Выбрать папку", this::pickFolder), 8);
        add(c, button("Создать текстовый / Markdown файл", () ->
                createFileDialog("dpLab-note.md", promptDraft)), 8);
        add(c, button("Открыть выбранный текстовый файл", this::pickTextFile), 8);
        add(body, c, 12);
    }

    private void pickFolder() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        startActivityForResult(i, PICK_FOLDER);
    }

    private void pickTextFile() {
        Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        i.setType("text/*");
        i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivityForResult(i, PICK_FILE);
    }

    private void createFileDialog(String defaultName, String initialText) {
        if (prefs.getString("folder_uri", null) == null) {
            toast("Сначала выбери папку");
            pickFolder();
            return;
        }
        LinearLayout form = column();
        form.setPadding(dp(16), dp(8), dp(16), 0);
        EditText filename = edit("Имя файла", 1, false);
        filename.setText(defaultName);
        EditText content = edit("Содержимое", 7, false);
        content.setText(initialText == null ? "" : initialText);
        add(form, filename, 0);
        add(form, content, 8);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Создать файл")
                .setView(form).setNegativeButton("Отмена", null)
                .setPositiveButton("Создать", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
            String name = filename.getText().toString().trim();
            if (name.isEmpty() || name.contains("/") || name.contains("\\")) {
                toast("Неправильное имя файла"); return;
            }
            try {
                Uri tree = Uri.parse(prefs.getString("folder_uri", null));
                Uri parent = DocumentsContract.buildDocumentUriUsingTree(
                        tree, DocumentsContract.getTreeDocumentId(tree));
                Uri file = DocumentsContract.createDocument(
                        getContentResolver(), parent, "text/plain", name);
                if (file == null) throw new IllegalStateException("Не удалось создать файл");
                try (OutputStream out = getContentResolver().openOutputStream(file, "w")) {
                    if (out == null) throw new IllegalStateException("Файл не открылся");
                    out.write(content.getText().toString().getBytes(StandardCharsets.UTF_8));
                }
                dialog.dismiss();
                toast("Создан файл: " + name);
            } catch (Exception e) {
                toast("Ошибка файла: " + shortError(e));
            }
        }));
        dialog.show();
    }

    private void readTextFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalStateException("Не удалось открыть документ");
            byte[] buffer = new byte[4096];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > 1024 * 1024) {
                    throw new IllegalStateException("Просмотр ограничен 1 МБ");
                }
                out.write(buffer, 0, count);
            }
            String content = out.toString(StandardCharsets.UTF_8.name());
            EditText preview = edit("Содержимое", 12, false);
            preview.setText(content);
            preview.setSelection(0);
            new AlertDialog.Builder(this).setTitle("Текстовый файл").setView(preview)
                    .setPositiveButton("Скопировать", (d, w) -> copy(content))
                    .setNegativeButton("Закрыть", null).show();
        } catch (Exception e) {
            toast("Ошибка чтения: " + shortError(e));
        }
    }

    @Override @SuppressWarnings("deprecation")
    protected void onActivityResult(int request, int result, Intent data) {
        super.onActivityResult(request, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        if (request == PICK_FOLDER) {
            int flags = data.getFlags() & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            try {
                getContentResolver().takePersistableUriPermission(uri, flags);
                prefs.edit().putString("folder_uri", uri.toString()).apply();
                toast("Папка подключена");
                render();
            } catch (Exception e) { toast("Не удалось сохранить папку: " + shortError(e)); }
        } else if (request == PICK_FILE) {
            readTextFile(uri);
        }
    }

    private void githubPage() {
        add(body, text("GitHub", 21, FG, true), 2);
        add(body, text("Прямое соединение с api.github.com. Токен шифруется Android Keystore и не отправляется на сторонний сервер.", 13, MUTED, false), 7);
        LinearLayout c = card("Подключить аккаунт", "Fine-grained token: Metadata read-only; Gists read/write — только если нужен Gist.");
        tokenField = edit("GitHub Personal Access Token", 1, true);
        add(c, tokenField, 10);
        add(c, button("Зашифрованно сохранить токен", this::saveToken), 8);
        add(c, button("Проверить и загрузить репозитории", this::checkGithub), 8);
        add(c, button("Создать приватный Gist", this::gistDialog), 8);
        add(c, button("Удалить токен", this::clearToken), 8);
        githubStatus = text("GitHub ещё не проверен", 12, MUTED, false);
        add(c, githubStatus, 10);
        reposList = text("", 12, FG, false);
        add(c, reposList, 8);
        add(body, c, 12);
    }

    private void saveToken() {
        String token = tokenField == null ? "" : tokenField.getText().toString().trim();
        if (token.isEmpty()) { toast("Вставь GitHub token"); return; }
        try {
            SecureTokenStore.save(this, token);
            tokenField.setText("");
            if (githubStatus != null) githubStatus.setText("Токен сохранён в Android Keystore");
            toast("Токен сохранён зашифрованно");
        } catch (Exception e) { toast("Ошибка сохранения: " + shortError(e)); }
    }

    private String getToken() {
        try { return SecureTokenStore.load(this); }
        catch (Exception e) { toast("Ошибка токена: " + shortError(e)); return null; }
    }

    private void clearToken() {
        SecureTokenStore.clear(this);
        if (githubStatus != null) githubStatus.setText("Токен удалён");
        if (reposList != null) reposList.setText("");
        toast("Токен удалён с устройства");
    }

    private void checkGithub() {
        String token = getToken();
        if (token == null || token.isEmpty()) { toast("Сначала сохрани токен"); return; }
        if (githubStatus != null) githubStatus.setText("Подключение к GitHub…");
        executor.execute(() -> {
            try {
                JSONObject user = GitHubClient.user(token);
                List<String> repos = GitHubClient.repositories(token);
                StringBuilder out = new StringBuilder("Репозитории:\n");
                for (String repo : repos) out.append("• ").append(repo).append('\n');
                if (repos.isEmpty()) out.append("Нет доступных репозиториев");
                String login = user.optString("login", "GitHub");
                runOnUiThread(() -> {
                    if (githubStatus != null) githubStatus.setText("Подключено: @" + login);
                    if (reposList != null) reposList.setText(out.toString());
                });
            } catch (Exception e) {
                String error = shortError(e);
                runOnUiThread(() -> { if (githubStatus != null) githubStatus.setText("Ошибка: " + error); });
            }
        });
    }

    private void gistDialog() {
        String token = getToken();
        if (token == null || token.isEmpty()) { toast("Сначала подключи GitHub"); return; }
        LinearLayout form = column();
        form.setPadding(dp(16), dp(8), dp(16), 0);
        EditText filename = edit("Имя файла", 1, false);
        filename.setText("note.md");
        EditText description = edit("Описание", 1, false);
        EditText content = edit("Содержимое", 6, false);
        add(form, filename, 0); add(form, description, 8); add(form, content, 8);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Приватный Gist")
                .setView(form).setNegativeButton("Отмена", null)
                .setPositiveButton("Создать", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
            String name = filename.getText().toString().trim();
            String desc = description.getText().toString().trim();
            String body = content.getText().toString();
            if (name.isEmpty() || name.contains("/") || body.isEmpty()) { toast("Заполни имя и содержимое"); return; }
            dialog.dismiss();
            toast("Создаю приватный Gist…");
            executor.execute(() -> {
                try {
                    String url = GitHubClient.createPrivateGist(token, name, desc, body);
                    runOnUiThread(() -> {
                        toast("Gist создан");
                        if (!url.isEmpty()) openUrl(url);
                    });
                } catch (Exception e) {
                    String error = shortError(e);
                    runOnUiThread(() -> toast("Ошибка Gist: " + error));
                }
            });
        }));
        dialog.show();
    }

    private void skillsPage() {
        add(body, text("Навыки", 21, FG, true), 2);
        add(body, text("Навык добавляет инструкции к задаче перед передачей в оригинальный обработчик DeepSeek.", 13, MUTED, false), 7);
        add(body, button("+ Новый навык", this::newSkillDialog), 10);
        for (Skill skill : skills) {
            LinearLayout c = card(skill.name,
                    (skill.id.equals(activeSkill) ? "● Активен · " : "")
                            + (skill.custom ? "Пользовательский" : "Встроенный"));
            add(c, text(skill.instruction, 12, MUTED, false), 8);
            add(c, button(skill.id.equals(activeSkill) ? "Выбран" : "Использовать", () -> {
                activeSkill = skill.id;
                prefs.edit().putString("active_skill", activeSkill).apply();
                render();
            }), 8);
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
                "Отвечай ясно и по делу. Разделяй факты и предположения.", false));
        skills.add(new Skill("code", "Разработка",
                "Действуй как опытный разработчик. Предлагай рабочий код и учитывай безопасность.", false));
        skills.add(new Skill("research", "Исследование",
                "Составь план исследования, опирайся на проверяемые источники и отмечай неопределённость.", false));
        skills.add(new Skill("writing", "Тексты",
                "Подготовь структурированный текст, сохраняя исходный смысл и не выдумывая факты.", false));
        try {
            JSONArray array = new JSONArray(prefs.getString("custom_skills", "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject item = array.getJSONObject(i);
                skills.add(new Skill(item.optString("id"), item.optString("name"),
                        item.optString("instruction"), true));
            }
        } catch (Exception ignored) {
            prefs.edit().putString("custom_skills", "[]").apply();
        }
    }

    private Skill selectedSkill() {
        for (Skill skill : skills) if (skill.id.equals(activeSkill)) return skill;
        return skills.get(0);
    }

    private void saveSkills() {
        JSONArray array = new JSONArray();
        try {
            for (Skill skill : skills) if (skill.custom) {
                JSONObject item = new JSONObject();
                item.put("id", skill.id);
                item.put("name", skill.name);
                item.put("instruction", skill.instruction);
                array.put(item);
            }
            prefs.edit().putString("custom_skills", array.toString()).apply();
        } catch (Exception ignored) {}
    }

    private void chooseSkill() {
        String[] names = new String[skills.size()];
        for (int i = 0; i < skills.size(); i++) names[i] = skills.get(i).name;
        new AlertDialog.Builder(this).setTitle("Выбрать навык").setItems(names, (d, which) -> {
            activeSkill = skills.get(which).id;
            prefs.edit().putString("active_skill", activeSkill).apply();
            if (tab.equals("ai")) render();
        }).setNegativeButton("Отмена", null).show();
    }

    private void newSkillDialog() {
        LinearLayout form = column();
        form.setPadding(dp(16), dp(8), dp(16), 0);
        EditText name = edit("Название навыка", 1, false);
        EditText instruction = edit("Инструкции для DeepSeek", 5, false);
        add(form, name, 0); add(form, instruction, 8);
        AlertDialog dialog = new AlertDialog.Builder(this).setTitle("Создать навык")
                .setView(form).setNegativeButton("Отмена", null)
                .setPositiveButton("Сохранить", null).create();
        dialog.setOnShowListener(v -> dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(w -> {
            String n = name.getText().toString().trim();
            String t = instruction.getText().toString().trim();
            if (n.isEmpty() || t.isEmpty()) { toast("Заполни оба поля"); return; }
            activeSkill = "custom-" + System.currentTimeMillis();
            skills.add(new Skill(activeSkill, n, t, true));
            saveSkills();
            prefs.edit().putString("active_skill", activeSkill).apply();
            dialog.dismiss();
            render();
        }));
        dialog.show();
    }

    private void copy(String value) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        clipboard.setPrimaryClip(ClipData.newPlainText("dpLab", value));
    }

    private void openUrl(String url) {
        try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url))); }
        catch (Exception e) { toast("Не удалось открыть ссылку"); }
    }

    private String shortError(Exception e) {
        String message = e.getMessage();
        if (message == null || message.trim().isEmpty()) message = e.getClass().getSimpleName();
        return message.length() > 140 ? message.substring(0, 140) : message;
    }

    private void toast(String value) {
        Toast.makeText(this, value, Toast.LENGTH_LONG).show();
    }
}
