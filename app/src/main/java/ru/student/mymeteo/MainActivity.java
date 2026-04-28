package ru.student.mymeteo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ru.student.mymeteo.data.DiaryStore;
import ru.student.mymeteo.data.ForecastRepository;
import ru.student.mymeteo.data.ProfileStore;
import ru.student.mymeteo.domain.DiaryEntry;
import ru.student.mymeteo.domain.ForecastBundle;
import ru.student.mymeteo.domain.RiskForecast;
import ru.student.mymeteo.domain.UserProfile;
import ru.student.mymeteo.notifications.MyMeteoNotifier;

public class MainActivity extends Activity {
    private static final int PRIMARY = Color.parseColor("#212842");
    private static final int BACKGROUND = Color.parseColor("#F0E7D5");
    private static final int SURFACE = Color.parseColor("#FFF9EF");
    private static final int MUTED = Color.parseColor("#687086");

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final ForecastRepository forecastRepository = new ForecastRepository();
    private final MyMeteoNotifier notifier = new MyMeteoNotifier();

    private ProfileStore profileStore;
    private DiaryStore diaryStore;
    private UserProfile profile;
    private ForecastBundle bundle;
    private LinearLayout content;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setStatusBarColor(PRIMARY);
        getWindow().setNavigationBarColor(PRIMARY);

        profileStore = new ProfileStore(this);
        diaryStore = new DiaryStore(this);
        profile = profileStore.load();
        notifier.ensureChannel(this);
        requestNotificationPermission();
        buildShell();
        showLoading();
        refreshForecast();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        TextView title = text("MyMeteo", 30, PRIMARY, Typeface.BOLD);
        title.setPadding(dp(20), dp(18), dp(20), dp(4));
        root.addView(title);

        TextView subtitle = text("Мягкий прогноз для метеочувствительных дней", 15, MUTED, Typeface.NORMAL);
        subtitle.setPadding(dp(20), 0, dp(20), dp(12));
        root.addView(subtitle);

        ScrollView scrollView = new ScrollView(this);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(4), dp(16), dp(16));
        scrollView.addView(content);
        root.addView(scrollView, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout nav = new LinearLayout(this);
        nav.setGravity(Gravity.CENTER);
        nav.setPadding(dp(8), dp(8), dp(8), dp(8));
        nav.setBackgroundColor(PRIMARY);
        nav.addView(navButton("Сегодня", view -> showHome()));
        nav.addView(navButton("Дневник", view -> showDiary()));
        nav.addView(navButton("Профиль", view -> showProfile()));
        nav.addView(navButton("Советы", view -> showRecommendations()));
        root.addView(nav);

        setContentView(root);
    }

    private Button navButton(String label, View.OnClickListener listener) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextColor(PRIMARY);
        button.setTextSize(13);
        button.setAllCaps(false);
        button.setBackground(round(Color.WHITE, dp(18)));
        button.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(44), 1);
        params.setMargins(dp(4), 0, dp(4), 0);
        button.setLayoutParams(params);
        return button;
    }

    private void showLoading() {
        content.removeAllViews();
        FrameLayout holder = new FrameLayout(this);
        ProgressBar progress = new ProgressBar(this);
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(dp(56), dp(56), Gravity.CENTER);
        holder.addView(progress, params);
        content.addView(holder, new LinearLayout.LayoutParams(-1, dp(180)));
    }

    private void refreshForecast() {
        executor.execute(() -> {
            ForecastBundle loaded = forecastRepository.load(profile);
            mainHandler.post(() -> {
                bundle = loaded;
                showHome();
                if (profile.notificationsEnabled) {
                    notifier.showRisk(this, loaded.risk);
                }
            });
        });
    }

    private void showHome() {
        content.removeAllViews();
        if (bundle == null) {
            showLoading();
            return;
        }

        RiskForecast risk = bundle.risk;
        LinearLayout status = card();
        status.setGravity(Gravity.CENTER_HORIZONTAL);
        TextView level = text(risk.level.title, 28, Color.WHITE, Typeface.BOLD);
        TextView score = text("Индекс риска: " + risk.score + "/100", 16, Color.WHITE, Typeface.NORMAL);
        status.setBackground(round(Color.parseColor(risk.level.colorHex), dp(24)));
        status.addView(level);
        status.addView(score);
        content.addView(status);

        LinearLayout metrics = card();
        metrics.addView(sectionTitle("Погодные показатели"));
        metrics.addView(metric("Город", profile.city));
        metrics.addView(metric("Температура", format(bundle.weather.currentTemperature) + " °C"));
        metrics.addView(metric("Перепад температуры", format(bundle.weather.temperatureDelta24h) + " °C / 24 ч"));
        metrics.addView(metric("Давление", format(bundle.weather.currentPressure) + " гПа"));
        metrics.addView(metric("Перепад давления", format(bundle.weather.pressureDelta24h) + " гПа / 24 ч"));
        metrics.addView(metric("Влажность", format(bundle.weather.currentHumidity) + "%"));
        metrics.addView(metric("Kp index", format(bundle.geomagnetic.maxKp)));
        metrics.addView(small("Источники: " + bundle.weather.source + ", " + bundle.geomagnetic.source));
        content.addView(metrics);

        LinearLayout reasons = card();
        reasons.addView(sectionTitle("Почему такой прогноз"));
        for (String reason : risk.reasons) {
            reasons.addView(bullet(reason));
        }
        content.addView(reasons);

        Button refresh = primaryButton("Обновить прогноз");
        refresh.setOnClickListener(view -> {
            showLoading();
            refreshForecast();
        });
        content.addView(refresh);
    }

    private void showDiary() {
        content.removeAllViews();
        LinearLayout add = card();
        add.addView(sectionTitle("Дневник самочувствия"));
        add.addView(small("Отмечайте состояние, чтобы позже найти личные погодные триггеры."));
        Button addButton = primaryButton("Добавить запись");
        addButton.setOnClickListener(view -> openDiaryDialog());
        add.addView(addButton);
        content.addView(add);

        List<DiaryEntry> entries = diaryStore.load();
        if (entries.isEmpty()) {
            content.addView(infoCard("Пока записей нет. Добавьте первую запись после прогулки, учебы или плохого самочувствия."));
            return;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
        for (DiaryEntry entry : entries) {
            LinearLayout row = card();
            row.addView(sectionTitle(format.format(new Date(entry.createdAt))));
            row.addView(metric("Самочувствие", entry.wellbeing + "/10"));
            row.addView(metric("Симптомы", entry.symptoms.isEmpty() ? "Не указаны" : entry.symptoms));
            if (!entry.note.isEmpty()) {
                row.addView(small(entry.note));
            }
            content.addView(row);
        }
    }

    private void openDiaryDialog() {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(8), dp(20), 0);

        TextView value = text("Самочувствие: 5/10", 16, PRIMARY, Typeface.BOLD);
        SeekBar seek = new SeekBar(this);
        seek.setMax(9);
        seek.setProgress(4);
        seek.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                value.setText("Самочувствие: " + (progress + 1) + "/10");
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) { }
            @Override public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        EditText symptoms = input("Симптомы: головная боль, усталость...");
        EditText note = input("Заметка");

        layout.addView(value);
        layout.addView(seek);
        layout.addView(symptoms);
        layout.addView(note);

        new AlertDialog.Builder(this)
                .setTitle("Новая запись")
                .setView(layout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    diaryStore.add(new DiaryEntry(
                            System.currentTimeMillis(),
                            seek.getProgress() + 1,
                            symptoms.getText().toString().trim(),
                            note.getText().toString().trim()
                    ));
                    showDiary();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void showProfile() {
        content.removeAllViews();
        LinearLayout card = card();
        card.addView(sectionTitle("Профиль"));
        card.addView(small("Отметьте факторы, которые могут усиливать персональный риск."));

        CheckBox hypertension = checkbox("Гипертония", profile.hypertension);
        CheckBox hypotension = checkbox("Гипотония", profile.hypotension);
        CheckBox migraine = checkbox("Мигрени", profile.migraine);
        CheckBox joints = checkbox("Боли в суставах", profile.joints);
        CheckBox magnetic = checkbox("Чувствительность к магнитным бурям", profile.magneticSensitive);
        CheckBox notifications = checkbox("Показывать предупреждения", profile.notificationsEnabled);

        card.addView(hypertension);
        card.addView(hypotension);
        card.addView(migraine);
        card.addView(joints);
        card.addView(magnetic);
        card.addView(notifications);

        Button save = primaryButton("Сохранить профиль");
        save.setOnClickListener(view -> {
            profile.hypertension = hypertension.isChecked();
            profile.hypotension = hypotension.isChecked();
            profile.migraine = migraine.isChecked();
            profile.joints = joints.isChecked();
            profile.magneticSensitive = magnetic.isChecked();
            profile.notificationsEnabled = notifications.isChecked();
            profileStore.save(profile);
            refreshForecast();
        });
        card.addView(save);
        content.addView(card);

        content.addView(infoCard("По умолчанию используется Москва. Координаты можно изменить в коде `UserProfile`, если нужен другой город для защиты проекта."));
    }

    private void showRecommendations() {
        content.removeAllViews();
        if (bundle == null) {
            showLoading();
            return;
        }
        LinearLayout card = card();
        card.addView(sectionTitle("Рекомендации на сегодня"));
        for (String recommendation : bundle.risk.recommendations) {
            card.addView(bullet(recommendation));
        }
        content.addView(card);

        Button notify = primaryButton("Показать тестовое уведомление");
        notify.setOnClickListener(view -> notifier.showRisk(this, bundle.risk));
        content.addView(notify);
    }

    private LinearLayout card() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(16), dp(18), dp(16));
        card.setBackground(round(SURFACE, dp(20)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, 0, 0, dp(14));
        card.setLayoutParams(params);
        return card;
    }

    private LinearLayout infoCard(String message) {
        LinearLayout card = card();
        card.addView(small(message));
        return card;
    }

    private TextView sectionTitle(String value) {
        TextView text = text(value, 20, PRIMARY, Typeface.BOLD);
        text.setPadding(0, 0, 0, dp(8));
        return text;
    }

    private TextView metric(String label, String value) {
        TextView text = text(label + ": " + value, 16, PRIMARY, Typeface.NORMAL);
        text.setPadding(0, dp(4), 0, dp(4));
        return text;
    }

    private TextView bullet(String value) {
        TextView text = text("• " + value, 15, PRIMARY, Typeface.NORMAL);
        text.setPadding(0, dp(4), 0, dp(4));
        return text;
    }

    private TextView small(String value) {
        TextView text = text(value, 14, MUTED, Typeface.NORMAL);
        text.setPadding(0, dp(6), 0, dp(6));
        return text;
    }

    private TextView text(String value, int sp, int color, int typeface) {
        TextView text = new TextView(this);
        text.setText(value);
        text.setTextSize(sp);
        text.setTextColor(color);
        text.setTypeface(Typeface.DEFAULT, typeface);
        text.setLineSpacing(2, 1.05f);
        return text;
    }

    private Button primaryButton(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(16);
        button.setTextColor(Color.WHITE);
        button.setBackground(round(PRIMARY, dp(18)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, dp(52));
        params.setMargins(0, dp(8), 0, 0);
        button.setLayoutParams(params);
        return button;
    }

    private CheckBox checkbox(String label, boolean checked) {
        CheckBox box = new CheckBox(this);
        box.setText(label);
        box.setTextColor(PRIMARY);
        box.setTextSize(16);
        box.setChecked(checked);
        box.setPadding(0, dp(4), 0, dp(4));
        return box;
    }

    private EditText input(String hint) {
        EditText editText = new EditText(this);
        editText.setHint(hint);
        editText.setTextColor(PRIMARY);
        editText.setHintTextColor(MUTED);
        return editText;
    }

    private GradientDrawable round(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(radius);
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private String format(double value) {
        return String.format(Locale.getDefault(), "%.1f", value);
    }
}
