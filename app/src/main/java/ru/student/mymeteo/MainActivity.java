package ru.student.mymeteo;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;
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
import ru.student.mymeteo.domain.FactorStatus;
import ru.student.mymeteo.domain.ForecastBundle;
import ru.student.mymeteo.domain.RiskForecast;
import ru.student.mymeteo.domain.UserProfile;
import ru.student.mymeteo.notifications.MyMeteoNotifier;
import ru.student.mymeteo.ui.FactorIconView;
import ru.student.mymeteo.ui.RiskTrendView;

public class MainActivity extends Activity {
    private static final int REQUEST_NOTIFICATIONS = 100;
    private static final int REQUEST_LOCATION = 101;
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
        buildShell();
        showLoading();
        requestLocationPermissionAndRefresh();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33
                && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_NOTIFICATIONS);
        }
    }

    private void requestLocationPermissionAndRefresh() {
        if (!hasLocationPermission()) {
            requestPermissions(new String[]{
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
            }, REQUEST_LOCATION);
            return;
        }
        updateLocationAndRefresh();
    }

    private boolean hasLocationPermission() {
        return checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void updateLocationAndRefresh() {
        if (!hasLocationPermission()) {
            requestForecastWithCurrentProfile();
            return;
        }
        Location location = findBestLastKnownLocation();
        if (location != null) {
            applyLocationAndRefresh(location);
            return;
        }
        requestFreshLocation();
    }

    private void requestForecastWithCurrentProfile() {
        requestNotificationPermission();
        refreshForecast();
    }

    private void applyLocationAndRefresh(Location location) {
        executor.execute(() -> {
            profile.latitude = location.getLatitude();
            profile.longitude = location.getLongitude();
            profile.city = detectCity(location);
            profileStore.save(profile);
            mainHandler.post(() -> {
                requestForecastWithCurrentProfile();
            });
        });
    }

    private void requestFreshLocation() {
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            requestForecastWithCurrentProfile();
            return;
        }
        List<String> providers = manager.getProviders(true);
        if (providers.isEmpty()) {
            requestForecastWithCurrentProfile();
            return;
        }
        String provider = providers.contains(LocationManager.NETWORK_PROVIDER)
                ? LocationManager.NETWORK_PROVIDER
                : providers.get(0);
        final Runnable[] timeout = new Runnable[1];
        LocationListener listener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                try {
                    manager.removeUpdates(this);
                } catch (SecurityException ignored) {
                }
                mainHandler.removeCallbacks(timeout[0]);
                applyLocationAndRefresh(location);
            }
        };
        timeout[0] = () -> {
            try {
                manager.removeUpdates(listener);
            } catch (SecurityException ignored) {
            }
            requestForecastWithCurrentProfile();
        };
        try {
            manager.requestSingleUpdate(provider, listener, Looper.getMainLooper());
            mainHandler.postDelayed(timeout[0], 8000);
        } catch (SecurityException ignored) {
            requestForecastWithCurrentProfile();
        } catch (IllegalArgumentException ignored) {
            requestForecastWithCurrentProfile();
        }
    }

    private Location findBestLastKnownLocation() {
        if (!hasLocationPermission()) {
            return null;
        }
        LocationManager manager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (manager == null) {
            return null;
        }
        Location best = null;
        List<String> providers = manager.getProviders(true);
        for (String provider : providers) {
            try {
                Location location = manager.getLastKnownLocation(provider);
                if (location == null) {
                    continue;
                }
                if (best == null
                        || location.getAccuracy() < best.getAccuracy()
                        || location.getTime() > best.getTime()) {
                    best = location;
                }
            } catch (SecurityException ignored) {
                return null;
            }
        }
        return best;
    }

    private String detectCity(Location location) {
        try {
            Geocoder geocoder = new Geocoder(this, Locale.getDefault());
            List<Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
            if (addresses == null || addresses.isEmpty()) {
                return "Текущее местоположение";
            }
            Address address = addresses.get(0);
            if (address.getLocality() != null && !address.getLocality().isEmpty()) {
                return address.getLocality();
            }
            if (address.getSubAdminArea() != null && !address.getSubAdminArea().isEmpty()) {
                return address.getSubAdminArea();
            }
        } catch (Exception ignored) {
        }
        return "Текущее местоположение";
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION) {
            updateLocationAndRefresh();
        }
    }

    private void buildShell() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BACKGROUND);

        TextView title = text("MyMeteo", 30, PRIMARY, Typeface.BOLD);
        title.setPadding(dp(20), dp(18), dp(20), dp(4));
        root.addView(title);

        TextView subtitle = text("Мягкий прогноз для метеочувствительных людей", 15, MUTED, Typeface.NORMAL);
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
        nav.addView(navButton("Советы", view -> showRecommendations()));
        nav.addView(profileNavButton());
        root.addView(nav);

        setContentView(root);
    }

    private void showDisclaimerIfNeeded() {
        if (profile.disclaimerAccepted) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Важное уведомление")
                .setMessage("MyMeteo не является медицинским изделием. Рекомендации носят информационный характер и не заменяют консультацию врача. Лекарства применяйте только по назначению специалиста.")
                .setPositiveButton("Понятно", (dialog, which) -> {
                    profile.disclaimerAccepted = true;
                    profileStore.save(profile);
                })
                .show();
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

    private ImageButton profileNavButton() {
        ImageButton button = new ImageButton(this);
        button.setImageResource(getResources().getIdentifier("ic_profile", "drawable", getPackageName()));
        button.setBackground(round(Color.WHITE, dp(18)));
        button.setScaleType(android.widget.ImageView.ScaleType.CENTER);
        button.setPadding(dp(8), dp(8), dp(8), dp(8));
        button.setContentDescription("Профиль");
        button.setOnClickListener(view -> showProfile());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(52), dp(44));
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
                showDisclaimerIfNeeded();
                if (shouldNotify(loaded.risk)) {
                    notifier.showRisk(this, loaded.risk);
                }
            });
        });
    }

    private boolean shouldNotify(RiskForecast risk) {
        if (!profile.notificationsEnabled) {
            return false;
        }
        if (risk.tags.contains("pressure") && profile.notifyPressure) {
            return true;
        }
        if (risk.tags.contains("temperature") && profile.notifyTemperature) {
            return true;
        }
        if (risk.tags.contains("humidity") && profile.notifyHumidity) {
            return true;
        }
        if (risk.tags.contains("geomagnetic") && profile.notifyGeomagnetic) {
            return true;
        }
        return risk.score >= 58;
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
        TextView score = text("Сегодня " + Math.max(1, risk.score / 10) + "/10 - индекс риска " + risk.score + "/100", 16, Color.WHITE, Typeface.NORMAL);
        status.setBackground(round(Color.parseColor(risk.level.colorHex), dp(24)));
        status.addView(level);
        status.addView(score);
        status.addView(text("Считывается одним взглядом: зеленый, желтый или мягкий красный уровень.", 13, Color.WHITE, Typeface.NORMAL));
        content.addView(status);

        LinearLayout trend = card();
        trend.addView(sectionTitle("Динамика индекса"));
        RiskTrendView chart = new RiskTrendView(this);
        chart.setValues(risk.trend);
        chart.setTimeline(
                bundle.weather.trendLabels,
                risk.trendNotes
        );
        chart.setOnPointClickListener(this::showFactorDetails);
        trend.addView(chart, new LinearLayout.LayoutParams(-1, dp(170)));
        if (profile.notifyTomorrow) {
            trend.addView(small("Нажмите на точку графика, чтобы открыть данные только за этот период."));
        }
        content.addView(trend);

        LinearLayout factors = card();
        factors.addView(sectionTitle("Факторы риска"));
        for (FactorStatus factor : risk.factors) {
            factors.addView(factorRow(factor));
        }
        content.addView(factors);

        LinearLayout metrics = card();
        metrics.addView(sectionTitleWithRefresh("Погодные показатели"));
        metrics.addView(metric("Город", profile.city));
        metrics.addView(metric("Температура", format(bundle.weather.currentTemperature) + " °C"));
        metrics.addView(metric("Перепад температуры", format(bundle.weather.temperatureDelta24h) + " °C / 24 ч"));
        metrics.addView(metric("Завтра: перепад температуры", format(bundle.weather.tomorrowTemperatureDelta) + " °C"));
        metrics.addView(metric("Давление", format(bundle.weather.currentPressure) + " мм рт. ст."));
        metrics.addView(metric("Перепад давления", format(bundle.weather.pressureDelta24h) + " мм рт. ст. / 24 ч"));
        metrics.addView(metric("Завтра: перепад давления", format(bundle.weather.tomorrowPressureDelta) + " мм рт. ст."));
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

    private void showFactorDetails(int index) {
        if (bundle == null) {
            return;
        }
        if (index < 0 || index >= bundle.weather.trendLabels.length) {
            index = 0;
        }
        StringBuilder builder = new StringBuilder();
        builder.append("Период: ")
                .append(bundle.weather.trendLabels[index])
                .append("\nИндекс: ")
                .append(bundle.risk.trend[index])
                .append("/100");
        if (bundle.risk.trendNotes[index] != null && !bundle.risk.trendNotes[index].isEmpty()) {
            builder.append("\nПричина: ").append(bundle.risk.trendNotes[index]);
        }
        builder.append("\n\nТемпература: ")
                .append(format(bundle.weather.trendTemperatures[index]))
                .append(" °C\nДавление: ")
                .append(format(bundle.weather.trendPressures[index]))
                .append(" мм рт. ст.\nВлажность: ")
                .append(format(bundle.weather.trendHumidity[index]))
                .append("%\nKp-индекс: ")
                .append(format(bundle.geomagnetic.maxKp));
        new AlertDialog.Builder(this)
                .setTitle("Детальные факторы")
                .setMessage(builder.toString().trim())
                .setPositiveButton("Понятно", null)
                .show();
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
        content.addView(diaryInsights(entries));
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
            row.addView(metric("Погода в момент записи", format(entry.temperature) + " °C, " + format(entry.pressure) + " мм рт. ст., Kp " + format(entry.kpIndex)));
            row.addView(metric("Индекс в момент записи", entry.riskScore + "/100, " + entry.riskLevel));
            if (!entry.note.isEmpty()) {
                row.addView(small(entry.note));
            }
            content.addView(row);
        }
    }

    private LinearLayout diaryInsights(List<DiaryEntry> entries) {
        LinearLayout card = card();
        card.addView(sectionTitle("Аналитика дневника"));
        if (entries.size() < 14) {
            card.addView(small("Для персонального вывода нужно 14-30 дней записей. Сейчас записей: " + entries.size() + "."));
        } else {
            int lowWellbeing = 0;
            int pressureDays = 0;
            int geomagneticDays = 0;
            for (DiaryEntry entry : entries) {
                if (entry.wellbeing <= 4) {
                    lowWellbeing++;
                    if (Math.abs(entry.pressure) > 0) {
                        pressureDays++;
                    }
                    if (entry.kpIndex >= 4) {
                        geomagneticDays++;
                    }
                }
            }
            card.addView(small("Плохое самочувствие отмечено " + lowWellbeing + " раз. Возможные персональные триггеры: давление - " + pressureDays + ", геомагнитная активность - " + geomagneticDays + "."));
        }
        card.addView(small("Запись автоматически связывается с температурой, давлением, влажностью, Kp-индексом и текущим риском."));
        return card;
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
        TextView symptomsTitle = text("Быстрый выбор симптомов", 16, PRIMARY, Typeface.BOLD);
        LinearLayout symptomGrid = new LinearLayout(this);
        symptomGrid.setOrientation(LinearLayout.VERTICAL);
        ArrayList<String> selectedSymptoms = new ArrayList<>();
        symptomGrid.addView(symptomButtonRow(selectedSymptoms, "Головная боль", "Сонливость"));
        symptomGrid.addView(symptomButtonRow(selectedSymptoms, "Суставы", "Давление"));
        symptomGrid.addView(symptomButtonRow(selectedSymptoms, "Усталость", "Тревожность"));
        EditText symptoms = input("Симптомы: головная боль, усталость...");
        EditText note = input("Заметка");

        layout.addView(value);
        layout.addView(seek);
        layout.addView(symptomsTitle);
        layout.addView(symptomGrid);
        layout.addView(symptoms);
        layout.addView(note);

        new AlertDialog.Builder(this)
                .setTitle("Новая запись")
                .setView(layout)
                .setPositiveButton("Сохранить", (dialog, which) -> {
                    String symptomText = joinSymptoms(selectedSymptoms, symptoms.getText().toString().trim());
                    diaryStore.add(new DiaryEntry(
                            System.currentTimeMillis(),
                            seek.getProgress() + 1,
                            symptomText,
                            note.getText().toString().trim(),
                            bundle == null ? 0 : bundle.weather.currentTemperature,
                            bundle == null ? 0 : bundle.weather.currentPressure,
                            bundle == null ? 0 : bundle.weather.currentHumidity,
                            bundle == null ? 0 : bundle.geomagnetic.maxKp,
                            bundle == null ? 0 : bundle.risk.score,
                            bundle == null ? "Нет прогноза" : bundle.risk.level.title
                    ));
                    showDiary();
                })
                .setNegativeButton("Отмена", null)
                .show();
    }

    private LinearLayout symptomButtonRow(ArrayList<String> selectedSymptoms, String first, String second) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.addView(symptomButton(selectedSymptoms, first));
        row.addView(symptomButton(selectedSymptoms, second));
        return row;
    }

    private Button symptomButton(ArrayList<String> selectedSymptoms, String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setAllCaps(false);
        button.setTextSize(14);
        button.setTextColor(PRIMARY);
        button.setBackground(round(Color.WHITE, dp(14)));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, dp(46), 1);
        params.setMargins(dp(3), dp(4), dp(3), dp(4));
        button.setLayoutParams(params);
        button.setOnClickListener(view -> {
            if (selectedSymptoms.contains(label)) {
                selectedSymptoms.remove(label);
                button.setTextColor(PRIMARY);
                button.setBackground(round(Color.WHITE, dp(14)));
            } else {
                selectedSymptoms.add(label);
                button.setTextColor(Color.WHITE);
                button.setBackground(round(PRIMARY, dp(14)));
            }
        });
        return button;
    }

    private String joinSymptoms(ArrayList<String> selectedSymptoms, String manual) {
        ArrayList<String> result = new ArrayList<>(selectedSymptoms);
        if (!manual.isEmpty()) {
            result.add(manual);
        }
        StringBuilder builder = new StringBuilder();
        for (int index = 0; index < result.size(); index++) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append(result.get(index));
        }
        return builder.toString();
    }

    private void showProfile() {
        content.removeAllViews();
        LinearLayout card = card();
        card.addView(sectionTitle("Профиль"));
        card.addView(small("Онбординг-анкета: возрастная группа, город и факторы, которые могут усиливать персональный риск."));

        EditText ageGroup = input("Возрастная группа");
        ageGroup.setText(profile.ageGroup);
        EditText city = input("Город");
        city.setText(profile.city);
        city.setEnabled(false);

        CheckBox hypertension = checkbox("Гипертония", profile.hypertension);
        CheckBox hypotension = checkbox("Гипотония", profile.hypotension);
        CheckBox migraine = checkbox("Мигрени", profile.migraine);
        CheckBox joints = checkbox("Боли в суставах", profile.joints);
        CheckBox magnetic = checkbox("Чувствительность к магнитным бурям", profile.magneticSensitive);
        CheckBox notifications = checkbox("Показывать предупреждения", profile.notificationsEnabled);
        CheckBox notifyPressure = checkbox("Уведомлять о скачках давления", profile.notifyPressure);
        CheckBox notifyTemperature = checkbox("Уведомлять о перепадах температуры", profile.notifyTemperature);
        CheckBox notifyHumidity = checkbox("Уведомлять о влажности", profile.notifyHumidity);
        CheckBox notifyGeomagnetic = checkbox("Уведомлять о магнитных бурях", profile.notifyGeomagnetic);
        CheckBox notifyTomorrow = checkbox("Показывать прогноз на завтра", profile.notifyTomorrow);

        card.addView(ageGroup);
        card.addView(city);
        card.addView(small("Координаты берутся автоматически после разрешения на геоданные."));
        card.addView(hypertension);
        card.addView(hypotension);
        card.addView(migraine);
        card.addView(joints);
        card.addView(magnetic);
        card.addView(notifications);
        card.addView(notifyPressure);
        card.addView(notifyTemperature);
        card.addView(notifyHumidity);
        card.addView(notifyGeomagnetic);
        card.addView(notifyTomorrow);

        Button save = primaryButton("Сохранить профиль");
        save.setOnClickListener(view -> {
            profile.ageGroup = ageGroup.getText().toString().trim().isEmpty() ? "18-30" : ageGroup.getText().toString().trim();
            profile.hypertension = hypertension.isChecked();
            profile.hypotension = hypotension.isChecked();
            profile.migraine = migraine.isChecked();
            profile.joints = joints.isChecked();
            profile.magneticSensitive = magnetic.isChecked();
            profile.notificationsEnabled = notifications.isChecked();
            profile.notifyPressure = notifyPressure.isChecked();
            profile.notifyTemperature = notifyTemperature.isChecked();
            profile.notifyHumidity = notifyHumidity.isChecked();
            profile.notifyGeomagnetic = notifyGeomagnetic.isChecked();
            profile.notifyTomorrow = notifyTomorrow.isChecked();
            profileStore.save(profile);
            refreshForecast();
        });
        card.addView(save);
        content.addView(card);

        content.addView(infoCard("Данные профиля и дневника хранятся локально на устройстве. Для учебного MVP аккаунты и отправка медицинских данных на сервер не используются."));
    }

    private void showRecommendations() {
        content.removeAllViews();
        if (bundle == null) {
            showLoading();
            return;
        }
        LinearLayout card = card();
        card.addView(sectionTitle("Рекомендации на сегодня"));
        card.addView(small("Советы подбираются по тэгам факторов: давление, температура, влажность, Kp-индекс и профиль."));
        for (String recommendation : bundle.risk.recommendations) {
            card.addView(bullet(recommendation));
        }
        content.addView(card);

        LinearLayout sources = card();
        sources.addView(sectionTitle("Источники рекомендаций"));
        sources.addView(bullet("ВОЗ: материалы по физической активности."));
        sources.addView(bullet("Минздрав РФ: рубрикатор клинических рекомендаций."));
        sources.addView(bullet("РЛС: справочник лекарственных средств для проверки формулировок."));
        sources.addView(small("Приложение не назначает препараты и не ставит диагнозы."));
        content.addView(sources);

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

    private LinearLayout sectionTitleWithRefresh(String value) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        TextView title = sectionTitle(value);
        row.addView(title, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout actionRow = new LinearLayout(this);
        actionRow.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        actionRow.setPadding(0, 0, 0, dp(8));
        TextView hint = small("Обновить погодные данные");
        actionRow.addView(hint, new LinearLayout.LayoutParams(0, -2, 1));
        ImageButton refresh = new ImageButton(this);
        refresh.setImageResource(getResources().getIdentifier("ic_refresh_button", "drawable", getPackageName()));
        refresh.setBackgroundColor(Color.TRANSPARENT);
        refresh.setScaleType(android.widget.ImageView.ScaleType.CENTER_INSIDE);
        refresh.setPadding(dp(4), dp(4), dp(4), dp(4));
        refresh.setContentDescription("Обновить погодные данные");
        refresh.setOnClickListener(view -> {
            showLoading();
            refreshForecast();
        });
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dp(44), dp(44));
        params.setMargins(dp(8), dp(4), dp(4), 0);
        actionRow.addView(refresh, params);
        row.addView(actionRow, new LinearLayout.LayoutParams(-1, -2));
        return row;
    }

    private TextView metric(String label, String value) {
        TextView text = text(label + ": " + value, 16, PRIMARY, Typeface.NORMAL);
        text.setPadding(0, dp(4), 0, dp(4));
        return text;
    }

    private LinearLayout factorRow(FactorStatus factor) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.setMargins(0, dp(4), 0, dp(6));
        row.setLayoutParams(params);
        row.setBackground(round(Color.WHITE, dp(14)));

        int accent = Color.parseColor(factor.level.colorHex);
        FactorIconView icon = new FactorIconView(this, factor.tag, accent);
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(54), dp(54));
        iconParams.setMargins(0, 0, dp(12), 0);
        row.addView(icon, iconParams);

        LinearLayout texts = new LinearLayout(this);
        texts.setOrientation(LinearLayout.VERTICAL);
        texts.addView(text(factor.title + ": " + factor.value, 16, PRIMARY, Typeface.BOLD));
        texts.addView(text(factor.explanation, 13, MUTED, Typeface.NORMAL));
        row.addView(texts, new LinearLayout.LayoutParams(0, -2, 1));
        return row;
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

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value.replace(',', '.').trim());
        } catch (Exception ignored) {
            return fallback;
        }
    }
}
