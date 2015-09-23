package org.wordpress.android.util;

import android.app.ActivityOptions;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.customtabs.CustomTabsClient;
import android.support.customtabs.CustomTabsIntent;
import android.support.customtabs.CustomTabsServiceConnection;
import android.text.TextUtils;

import org.wordpress.android.R;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.passcodelock.AppLockManager;

public class CustomTabsUtils {

    private static Boolean mIsCustomTabsSupported;

    private static boolean isCustomTabsSupported(Context context) {
        if (mIsCustomTabsSupported == null) {
            String packageNameToBind = CustomTabsHelper.getPackageNameToUse(context);
            if (packageNameToBind == null) {
                mIsCustomTabsSupported = false;
                return false;
            }

            CustomTabsServiceConnection connection = new CustomTabsServiceConnection() {
                @Override
                public void onCustomTabsServiceConnected(ComponentName name, CustomTabsClient client) {
                }

                @Override
                public void onServiceDisconnected(ComponentName name) {
                }
            };

            mIsCustomTabsSupported = CustomTabsClient.bindCustomTabsService(context, packageNameToBind, connection);
            if (mIsCustomTabsSupported) {
                context.unbindService(connection);
            }
        }

        return mIsCustomTabsSupported;
    }

    public enum OpenUrlType {
        // show pages in-app even if custom tabs aren't supported
        INTERNAL,
        // show pages in-app only if custom tabs are supported (external browser otherwise)
        INTERNAL_IF_CUSTOM_TABS_SUPPORTED,
        // show pages in external browser
        EXTERNAL
    }

    public static void openUrl(Context context, String url) {
        openUrl(context, url, OpenUrlType.INTERNAL);
    }

    public static void openUrl(Context context, String url, OpenUrlType openUrlType) {
        if (context == null || TextUtils.isEmpty(url)) return;

        boolean isExternal;
        switch (openUrlType) {
            case EXTERNAL:
                isExternal = true;
                break;
            case INTERNAL_IF_CUSTOM_TABS_SUPPORTED:
                isExternal = !isCustomTabsSupported(context);
                break;
            default:
                isExternal = false;
                break;
        }

        if (isExternal) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                context.startActivity(intent);
                AppLockManager.getInstance().setExtendedTimeout();
            } catch (ActivityNotFoundException e) {
                String errString = context.getString(R.string.reader_toast_err_url_intent);
                ToastUtils.showToast(context, String.format(errString, url), ToastUtils.Duration.LONG);
            }
        } else if (isCustomTabsSupported(context)) {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.putExtra(CustomTabsIntent.EXTRA_TITLE_VISIBILITY_STATE, CustomTabsIntent.SHOW_PAGE_TITLE);
            CustomTabsHelper.addKeepAliveExtra(context, intent);

            Bundle extras = new Bundle();
            extras.putBinder(CustomTabsIntent.EXTRA_SESSION, null);
            intent.putExtras(extras);

            Bitmap icon = BitmapFactory.decodeResource(
                    context.getResources(), R.drawable.ic_arrow_back_black_24dp);
            intent.putExtra(CustomTabsIntent.EXTRA_CLOSE_BUTTON_ICON, icon);

            Bundle finishBundle = ActivityOptions.makeCustomAnimation(
                    context, R.anim.do_nothing, R.anim.activity_slide_out_to_right).toBundle();
            intent.putExtra(CustomTabsIntent.EXTRA_EXIT_ANIMATION_BUNDLE, finishBundle);

            Bundle startBundle = ActivityOptions.makeCustomAnimation(
                    context, R.anim.activity_slide_in_from_right, R.anim.do_nothing).toBundle();

            context.startActivity(intent, startBundle);
        } else {
            WPWebViewActivity.openURL(context, url);
        }
    }
}