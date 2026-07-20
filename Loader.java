package developer.by.sawar;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.io.File;

public class Loader {

    static { System.loadLibrary("loader"); }

    private static Dialog dialog;
    private static LinearLayout card;
    private static View cardStack; // الحاوية المربعة الكاملة (صورة + تدرج + محتوى)
    private static TextView title, status, percent, telegram;
    private static IOSSpinnerView spinner;

    private static final Handler ui = new Handler(Looper.getMainLooper());

    /* progress state */
    private static int rawPercent = 0;
    private static int displayPercent = 0;
    private static boolean springRunning = false;
    private static boolean started = false;

    /* JNI */
    private static native void nativeStart(Activity activity);
    public static native String getNativeString(int id);

    /* ================= JNI CALLBACKS ================= */

    public static void onStatus(final String msg) {
        ui.post(new Runnable() {
            @Override public void run() {
                crossFadeText(status, msg, Color.parseColor("#FFD400"));
            }
        });
    }

    public static void onProgress(final int p) {
        ui.post(new Runnable() {
            @Override public void run() {
                if (percent == null || springRunning) return;

                rawPercent = p;
                int eased = applyEaseOut(rawPercent);

                if (eased > displayPercent) {
                    displayPercent = eased;
                    percent.setText(displayPercent + getNativeString(12));
                    if (spinner != null) spinner.setProgress(displayPercent);
                }
            }
        });
    }

    public static void clearCache(Activity a) {
        if (a == null) return;

        try {
            File dir = a.getCacheDir();
            if (dir != null && dir.isDirectory()) {
                File[] files = dir.listFiles();
                if (files != null) {
                    for (File f : files) {
                        if (f.getName().startsWith(".rrq_")) {
                            f.delete();
                        }
                    }
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void onAppBackground() {
        started = false;
        springRunning = false;
    }

    public static void onError(final String msg) {
        ui.post(new Runnable() {
            @Override public void run() {
                crossFadeText(status, msg, Color.parseColor("#FF3B3B"));
            }
        });
    }

    public static void onSuccess() {
        ui.post(new Runnable() {
            @Override public void run() {
                crossFadeText(status, getNativeString(5), Color.parseColor("#00FF88"));
                startSpringFinish();
            }
        });
    }

    /* ================= iOS EASING ================= */

    private static int applyEaseOut(int p) {
        if (p <= 85) return p;

        float t = (p - 85) / 15f;
        float eased = 1f - (float)Math.pow(1f - t, 3);
        return 85 + Math.round(eased * 15f);
    }

    /* ================= iOS SPRING FINISH ================= */

    private static void startSpringFinish() {
        springRunning = true;

        ui.post(new Runnable() {
            @Override public void run() {
                if (percent == null) return;

                displayPercent = 100;
                percent.setText(getNativeString(11));
                if (spinner != null) spinner.setProgress(100);

                springRunning = false;

                ui.postDelayed(new Runnable() {
                    @Override public void run() {
                        fadeOutAndDismiss(cardStack);
                    }
                }, 350);
            }
        });
    }

    /* ================= ANIM HELPERS ================= */

    private static void fadeIn(View v) {
        v.setAlpha(0f);
        v.setScaleX(0.92f);
        v.setScaleY(0.92f);
        v.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(260)
            .setInterpolator(new android.view.animation.OvershootInterpolator(0.9f))
            .start();
    }

    private static void fadeOutAndDismiss(final View v) {
        v.animate().alpha(0f).scaleX(0.94f).scaleY(0.94f).setDuration(220).withEndAction(new Runnable() {
            @Override public void run() {
                if (dialog != null && dialog.isShowing()) {
                    dialog.dismiss();
                    dialog = null;
                }
            }
        }).start();
    }

    private static void crossFadeText(final TextView tv, final String text, final int color) {
        if (tv == null) return;

        tv.animate().alpha(0f).setDuration(120).withEndAction(new Runnable() {
            @Override public void run() {
                tv.setText(text);
                tv.setTextColor(color);
                tv.animate().alpha(1f).setDuration(160).start();
            }
        }).start();
    }

    /* ================= ENTRY ================= */

    public static void init(final Activity a) {
        if (started) return;
        started = true;

        showUI(a);
        new Thread(new Runnable() {
            @Override public void run() {
                nativeStart(a);
            }
        }).start();
    }

    /* ================= UI (تصميم فاخر Luxury Dark Gold) ================= */

    private static void showUI(final Activity a) {
        if (a == null || a.isFinishing()) return;

        final float d = a.getResources().getDisplayMetrics().density;

        final int GOLD_DEEP    = Color.parseColor("#9C7A22");
        final int GOLD         = Color.parseColor("#D4AF37");
        final int GOLD_BRIGHT  = Color.parseColor("#F7E3A1");
        final int IVORY        = Color.parseColor("#F3E9D2");
        final int STATUS_COLOR = Color.parseColor("#B8A98A");
        final int BORDER_COLOR = Color.parseColor("#4DD4AF37");
        final int CORNER       = (int)(22 * d);

        int screenW = a.getResources().getDisplayMetrics().widthPixels;
        int squareSize = Math.min((int)(238 * d), (int)(screenW * 0.66f));

        // ===== الحاوية (خلفية سوداء دافئة + صورة + سكريم ذهبي + حدود) =====
        FrameLayout square = new FrameLayout(a);
        square.setClipToOutline(true);
        square.setOutlineProvider(new ViewOutlineProvider() {
            @Override public void getOutline(View view, Outline outline) {
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), CORNER);
            }
        });

        GradientDrawable baseBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{Color.parseColor("#1C160A"), Color.parseColor("#080704")}
        );
        baseBg.setCornerRadius(CORNER);
        square.setBackground(baseBg);

        ImageView bgImage = new ImageView(a);
        bgImage.setScaleType(ImageView.ScaleType.CENTER_CROP);
        bgImage.setAlpha(0.20f);
        int imgResId = a.getResources().getIdentifier("MY_", "drawable", a.getPackageName());
        if (imgResId != 0) {
            bgImage.setImageResource(imgResId);
        }
        square.addView(bgImage, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // سكريم دافئ (بني غامق -> أسود) بدل الأرجواني القديم
        View scrim = new View(a);
        GradientDrawable scrimBg = new GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            new int[]{
                Color.parseColor("#B3120D06"),
                Color.parseColor("#D90A0703"),
                Color.parseColor("#F2050402")
            }
        );
        scrim.setBackground(scrimBg);
        square.addView(scrim, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        // حدّ ذهبي رفيع أنيق حول البطاقة كاملة
        View borderOverlay = new View(a);
        GradientDrawable borderBg = new GradientDrawable();
        borderBg.setColor(Color.TRANSPARENT);
        borderBg.setCornerRadius(CORNER);
        borderBg.setStroke((int)(1.2f * d), BORDER_COLOR);
        borderOverlay.setBackground(borderBg);
        square.addView(borderOverlay, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            square.setElevation(22 * d);
        }
        // ظل ذهبي فاخر حول البطاقة (Android 9+)
        if (Build.VERSION.SDK_INT >= 28) {
            try {
                square.setOutlineAmbientShadowColor(GOLD_DEEP);
                square.setOutlineSpotShadowColor(GOLD);
            } catch (Throwable ignored) {}
        }

        // ----- المحتوى -----
        card = new LinearLayout(a);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        int padH = (int)(22 * d);
        card.setPadding(padH, padH, padH, padH);
        square.addView(card, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT, Gravity.CENTER));

        // العنوان (خط serif فاخر + تباعد أحرف)
        title = new TextView(a);
        title.setText(getNativeString(1));
        title.setTextColor(IVORY);
        title.setTextSize(16.5f);
        title.setLetterSpacing(0.05f);
        title.setTypeface(android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD));
        title.setGravity(Gravity.CENTER);
        card.addView(title);

        // ----- فاصل زخرفي: خط - معين ذهبي - خط (لمسة فخامة) -----
        LinearLayout divider = new LinearLayout(a);
        divider.setOrientation(LinearLayout.HORIZONTAL);
        divider.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams dividerParams = new LinearLayout.LayoutParams(
            (int)(86 * d), ViewGroup.LayoutParams.WRAP_CONTENT);
        dividerParams.topMargin = (int)(8 * d);
        dividerParams.bottomMargin = (int)(16 * d);
        card.addView(divider, dividerParams);

        View lineL = new View(a);
        lineL.setBackgroundColor(BORDER_COLOR);
        LinearLayout.LayoutParams lineLParams = new LinearLayout.LayoutParams(0, (int)(1 * d), 1f);
        divider.addView(lineL, lineLParams);

        View diamond = new View(a);
        GradientDrawable diamondBg = new GradientDrawable();
        diamondBg.setColor(GOLD);
        diamond.setBackground(diamondBg);
        diamond.setRotation(45f);
        LinearLayout.LayoutParams diamondParams = new LinearLayout.LayoutParams((int)(5 * d), (int)(5 * d));
        diamondParams.leftMargin = (int)(6 * d);
        diamondParams.rightMargin = (int)(6 * d);
        divider.addView(diamond, diamondParams);

        View lineR = new View(a);
        lineR.setBackgroundColor(BORDER_COLOR);
        LinearLayout.LayoutParams lineRParams = new LinearLayout.LayoutParams(0, (int)(1 * d), 1f);
        divider.addView(lineR, lineRParams);

        // ----- حلقة التقدّم الذهبية + النسبة داخلها -----
        FrameLayout ringStack = new FrameLayout(a);
        int ringSize = (int)(86 * d);
        LinearLayout.LayoutParams ringParams = new LinearLayout.LayoutParams(ringSize, ringSize);
        ringParams.bottomMargin = (int)(16 * d);
        card.addView(ringStack, ringParams);

        spinner = new IOSSpinnerView(a);
        ringStack.addView(spinner, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        percent = new TextView(a);
        percent.setText(getNativeString(10));
        percent.setTextColor(GOLD_BRIGHT);
        percent.setTextSize(18);
        percent.setTypeface(android.graphics.Typeface.create("serif", android.graphics.Typeface.BOLD));
        percent.setGravity(Gravity.CENTER);
        ringStack.addView(percent, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));

        // الحالة
        status = new TextView(a);
        status.setText(getNativeString(2));
        status.setTextColor(STATUS_COLOR);
        status.setGravity(Gravity.CENTER);
        status.setMaxLines(2);
        status.setTextSize(11.5f);
        status.setLetterSpacing(0.01f);
        status.setPadding((int)(4 * d), 0, (int)(4 * d), 0);
        card.addView(status);

        // ----- زر ذهبي فاخر (تعبئة متدرجة + نص داكن للتباين الفخم) -----
        telegram = new TextView(a);
        telegram.setText(getNativeString(6));
        telegram.setTextColor(Color.parseColor("#1B140A"));
        telegram.setTextSize(11.5f);
        telegram.setLetterSpacing(0.04f);
        telegram.setTypeface(telegram.getTypeface(), android.graphics.Typeface.BOLD);
        telegram.setGravity(Gravity.CENTER);
        telegram.setClickable(true);
        telegram.setMinHeight(0);

        GradientDrawable btnBg = new GradientDrawable(
            GradientDrawable.Orientation.LEFT_RIGHT,
            new int[]{GOLD_DEEP, GOLD, GOLD_BRIGHT}
        );
        btnBg.setCornerRadius(16 * d);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ColorDrawable rippleMask = new ColorDrawable(Color.WHITE);
            RippleDrawable ripple = new RippleDrawable(
                ColorStateList.valueOf(Color.parseColor("#40FFFFFF")),
                btnBg,
                rippleMask
            );
            telegram.setBackground(ripple);
        } else {
            telegram.setBackground(btnBg);
        }

        int hPadBtn = (int)(22 * d);
        int vPadBtn = (int)(10 * d);
        telegram.setPadding(hPadBtn, vPadBtn, hPadBtn, vPadBtn);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            telegram.setElevation(6 * d);
        }

        telegram.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(getNativeString(7)));
                a.startActivity(i);
            }
        });

        LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        btnParams.topMargin = (int)(16 * d);
        card.addView(telegram, btnParams);

        // ===== Root =====
        FrameLayout root = new FrameLayout(a);
        root.setBackgroundColor(0xD9060503);

        FrameLayout.LayoutParams squareParams = new FrameLayout.LayoutParams(squareSize, squareSize, Gravity.CENTER);
        root.addView(square, squareParams);
        cardStack = square;

        // ----- Dialog -----
        dialog = new Dialog(a, android.R.style.Theme_Translucent_NoTitleBar);
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.setContentView(root);
        dialog.setOnKeyListener(new DialogInterface.OnKeyListener() {
            @Override public boolean onKey(DialogInterface d, int k, KeyEvent e) {
                return k == KeyEvent.KEYCODE_BACK;
            }
        });
        dialog.show();

        fadeIn(square);
    }
}