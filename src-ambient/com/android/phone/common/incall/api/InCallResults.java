package com.android.phone.common.incall.api;

import android.content.ComponentName;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;

import com.android.phone.common.incall.CallMethodInfo;
import com.android.phone.common.incall.utils.CallMethodUtils;
import com.android.phone.common.ambient.utils.ModUtils;
import com.android.phone.common.nudge.api.NudgeTypedResult;
import com.cyanogen.ambient.discovery.results.BundleResult;
import com.cyanogen.ambient.discovery.util.NudgeKey;
import com.cyanogen.ambient.incall.extension.CreditBalance;
import com.cyanogen.ambient.incall.extension.CreditInfo;
import com.cyanogen.ambient.incall.extension.GetCreditInfoResult;
import com.cyanogen.ambient.incall.extension.HintTextResult;
import com.cyanogen.ambient.incall.extension.StatusCodes;
import com.cyanogen.ambient.incall.results.AccountHandleResult;
import com.cyanogen.ambient.incall.results.AuthenticationStateResult;
import com.cyanogen.ambient.incall.results.HintTextResultResult;
import com.cyanogen.ambient.incall.results.InCallProviderInfoResult;
import com.cyanogen.ambient.incall.results.InstalledPluginsResult;
import com.cyanogen.ambient.incall.results.MimeTypeResult;
import com.cyanogen.ambient.incall.results.PendingIntentResult;
import com.cyanogen.ambient.incall.results.PluginStatusResult;
import com.cyanogen.ambient.incall.util.InCallProviderInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * Class that handles writing results to our CallMethodInfo object
 */
public class InCallResults extends ApiHelper {

    private static final String TAG = InCallResults.class.getSimpleName();

    public static List<ComponentName> gotInstalledPlugins(InstalledPluginsResult result) {
        if (result.components == null) {
            new ArrayList<ComponentName>();
        }
        return result.components;
    }

    public static void gotStatus(CallMethodInfo callMethodInfo, PluginStatusResult result) {
        callMethodInfo.mStatus = result.status;
    }

    public static void gotMimeType(CallMethodInfo callMethodInfo, MimeTypeResult result, int type) {
        switch(type) {
            case InCallTypedResult.GENERAL_MIME_TYPE:
                callMethodInfo.mMimeType = result.mimeType;
                break;
            case InCallTypedResult.VIDEO_MIME_TYPE:
                callMethodInfo.mVideoCallableMimeType = result.mimeType;
                break;
            case InCallTypedResult.IM_MIME_TYPE:
                callMethodInfo.mImMimeType = result.mimeType;
                break;
        }
    }

    public static void gotIntent(CallMethodInfo callMethodInfo, PendingIntentResult result,
            int type) {
        switch(type) {
            case InCallTypedResult.LOGIN_INTENT:
                callMethodInfo.mLoginIntent = result.intent;
                break;
            case InCallTypedResult.CREDIT_INTENT:
                callMethodInfo.mManageCreditIntent = result.intent;
                break;
            case InCallTypedResult.SETTINGS_INTENT:
                callMethodInfo.mSettingsIntent = result.intent;
                break;
            case InCallTypedResult.DEFAULT_DIRECTORY_SEARCH_INTENT:
                callMethodInfo.mDefaultDirectorySearchIntent = result.intent;
                break;
        }
    }

    public static void gotAuthenticationState(CallMethodInfo callMethodInfo,
            AuthenticationStateResult result) {
        callMethodInfo.mIsAuthenticated
                = result.result == StatusCodes.AuthenticationState.LOGGED_IN;
    }

    public static void gotAccountHandle(CallMethodInfo callMethodInfo, Context context,
            AccountHandleResult result) {
        gotAccountHandle(callMethodInfo, context, result.accountHandle);
    }

    private static void gotAccountHandle(CallMethodInfo callMethodInfo, Context context,
            String accountHandle) {
        callMethodInfo.mAccountHandle = accountHandle;
        if (TextUtils.isEmpty(callMethodInfo.mAccountHandle)) {
            callMethodInfo.mAccountHandle
                    = CallMethodUtils.lookupAccountHandle(context, callMethodInfo.mAccountType);
        }
    }

    public static void gotGeneralInfo(CallMethodInfo callMethodInfo, Context context,
            InCallProviderInfoResult result) {

        ComponentName componentName = callMethodInfo.mComponent;
        InCallProviderInfo icpi = result.inCallProviderInfo;
        if (icpi == null) {
            getInstance(context).getPluginInfo().remove(componentName);
            return;
        }

        Resources resources = ModUtils.getPluginResources(context, componentName);
        if (resources == null) {
            return;
        }

        callMethodInfo.mBrandIconId = icpi.getBrandIcon();
        callMethodInfo.mLoginIconId = icpi.getLoginIcon();

        try {
            callMethodInfo.mSingleColorBrandIcon
                    = resources.getDrawable(icpi.getSingleColorBrandIcon());
            callMethodInfo.mBrandIcon = resources.getDrawable(callMethodInfo.mBrandIconId);
            callMethodInfo.mLoginIcon = resources.getDrawable(callMethodInfo.mLoginIconId);
            callMethodInfo.mVoiceIcon = resources.getDrawable(icpi.getVoiceMimeIcon());
            callMethodInfo.mImIcon = resources.getDrawable(icpi.getImMimeIcon());
            callMethodInfo.mBadgeIcon = resources.getDrawable(icpi.getBadgeIcon());
        } catch (Resources.NotFoundException e) {
            Log.e(TAG, "Resource Not found: " + componentName.flattenToShortString());
            getInstance(context).getPluginInfo().remove(componentName);
            return;
        }

        callMethodInfo.mSubscriptionButtonText = icpi.getSubscriptionButtonText();
        callMethodInfo.mCreditButtonText = icpi.getCreditsButtonText();
        // If present, use the deprecated attribute defined hint text.
        // These values may be overwritten by getHintText.
        callMethodInfo.mT9HintDescriptionNoCreditOrSub = icpi.getT9HintDescription();
        callMethodInfo.mT9HintDescriptionHasCreditOrSub = icpi.getT9HintDescription();
        callMethodInfo.mDependentPackage = icpi.getDependentPackage();
        callMethodInfo.mName = icpi.getTitle();
        callMethodInfo.mSummary = icpi.getSummary();
        callMethodInfo.mAccountType = icpi.getAccountType();
        gotAccountHandle(callMethodInfo, context, icpi.getAccountHandle());
    }

    public static void gotCreditData(CallMethodInfo callMethodInfo, GetCreditInfoResult result) {
        if (result.creditInfo == null) {
            // Build zero credit dummy if no result found.
            callMethodInfo.mProviderCreditInfo = new CreditInfo(new CreditBalance(0, null), null);
        } else {
            callMethodInfo.mProviderCreditInfo = result.creditInfo;
        }
    }

    public static void gotHintText(CallMethodInfo callMethodInfo,
            HintTextResultResult resultResult) {
        HintTextResult result = resultResult.result;
        if (result != null) {
            String hintText = result.getNoCreditOrSubscriptionHint();
            if (!TextUtils.isEmpty(hintText)) {
                callMethodInfo.mT9HintDescriptionNoCreditOrSub = hintText;
            }
            hintText = result.getHasCreditOrSubscriptionHint();
            if (!TextUtils.isEmpty(hintText)) {
                callMethodInfo.mT9HintDescriptionHasCreditOrSub = hintText;
            }
        }
    }

    public static void updateCreditInfo(Context context, ComponentName name,
            GetCreditInfoResult result) {
        CallMethodInfo callMethodInfo = getInstance(context).getPluginIfExists(name);
        if (callMethodInfo != null) {
            gotCreditData(callMethodInfo, result);
            getInstance(context).getPluginInfo().put(name, callMethodInfo);

            // Since a CallMethodInfo object was updated here, we should let the subscribers know
            getInstance(context).broadcast();
        } else {
            Log.wtf(TAG, "Call method does not exist, this should not happen");
        }
    }

    public static void updateAuthenticationState(Context context, ComponentName name, int state) {
        CallMethodInfo callMethodInfo = getInstance(context).getPluginIfExists(name);
        if (callMethodInfo != null) {
            callMethodInfo.mIsAuthenticated = state == StatusCodes.AuthenticationState.LOGGED_IN;
            getInstance(context).getPluginInfo().put(name, callMethodInfo);

            // Since a CallMethodInfo object was updated here, we should let the subscribers know
            getInstance(context).broadcast();
        } else {
            Log.wtf(TAG, "Call method does not exist, this should not happen");
        }
    }

    public static void gotNudgeData(Context context, CallMethodInfo callMethodInfo,
            BundleResult result, int type) {

        Bundle bundle = result.bundle;
        if (bundle == null) {
            return;
        }
        switch (type) {
            case NudgeTypedResult.INCALL_CREDIT_NUDGE:
                gotCreditWarn(callMethodInfo, bundle);
                break;
            case NudgeTypedResult.INCALL_CONTACT_FRAGMENT_LOGIN:
                gotLoginSubtitle(callMethodInfo, bundle);
                break;
            case NudgeTypedResult.INCALL_CONTACT_CARD_LOGIN:
                gotContactCardLogin(callMethodInfo, NudgeKey.INCALL_CONTACT_CARD_LOGIN, bundle,
                        context);
                break;
            case NudgeTypedResult.INCALL_CONTACT_CARD_DOWNLOAD:
                gotContactCardDownload(callMethodInfo, NudgeKey.INCALL_CONTACT_CARD_DOWNLOAD,
                        bundle, context);
                break;
        }
    }

    private static void gotLoginSubtitle(CallMethodInfo callMethodInfo, Bundle bundle) {
        callMethodInfo.mLoginSubtitle = bundle.getString(NudgeKey.NUDGE_PARAM_SUBTITLE);
    }

    private static void gotContactCardLogin(CallMethodInfo callMethodInfo, String key,
            Bundle bundle, Context c) {
        callMethodInfo.mLoginNudgeEnable = bundle.getBoolean(NudgeKey.NUDGE_PARAM_ENABLED, true)
                && PreferenceManager.getDefaultSharedPreferences(c).getBoolean(
                callMethodInfo.mComponent.getClassName() + "." + key, true);
        callMethodInfo.mLoginNudgeSubtitle = bundle.getString(NudgeKey.NUDGE_PARAM_SUBTITLE);
        callMethodInfo.mLoginNudgeActionText = bundle.getString(NudgeKey.NUDGE_PARAM_ACTION_TEXT);
    }

    private static void gotCreditWarn(CallMethodInfo callMethodInfo, Bundle bundle) {
        Object creditWarn = bundle.get(NudgeKey.INCALL_PARAM_CREDIT_WARN);
        if (creditWarn.getClass().equals(Integer.class)) {
            callMethodInfo.mCreditWarn = (Integer) creditWarn;
        } else if (creditWarn.getClass().equals(Float.class)) {
            callMethodInfo.mCreditWarn = (Float) creditWarn;
        } else {
            Log.e(TAG, "Invalid value for Credit Warn limit: " + creditWarn);
        }
    }

    private static void gotContactCardDownload(CallMethodInfo callMethodInfo, String key,
            Bundle bundle, Context context) {
        callMethodInfo.mInstallNudgeEnable = bundle.getBoolean(NudgeKey.NUDGE_PARAM_ENABLED, true)
                && PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                callMethodInfo.mComponent.getClassName() + "." + key, true);
        callMethodInfo.mInstallNudgeSubtitle = bundle.getString(NudgeKey.NUDGE_PARAM_SUBTITLE);
        callMethodInfo.mInstallNudgeActionText = bundle.getString(NudgeKey.NUDGE_PARAM_ACTION_TEXT);
    }
}
