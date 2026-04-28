package ru.student.mymeteo.notifications;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;

import ru.student.mymeteo.domain.RiskForecast;
import ru.student.mymeteo.domain.RiskLevel;

public class MyMeteoNotifier {
    private static final String CHANNEL_ID = "risk_alerts";

    public void ensureChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Метеопредупреждения",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("Предупреждения о погодных и геомагнитных рисках");
        channel.setLightColor(Color.parseColor("#212842"));
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager != null) {
            manager.createNotificationChannel(channel);
        }
    }

    public void showRisk(Context context, RiskForecast forecast) {
        ensureChannel(context);
        if (forecast.level == RiskLevel.LOW) {
            return;
        }
        String text = forecast.reasons.isEmpty()
                ? "Проверьте прогноз и рекомендации в MyMeteo."
                : forecast.reasons.get(0);

        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);

        builder.setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(forecast.level.title)
                .setContentText(text)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(text))
                .setAutoCancel(true);

        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (manager != null) {
            manager.notify(42, builder.build());
        }
    }
}
