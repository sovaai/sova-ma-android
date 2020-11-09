package com.osmino.sova.preference;

import static android.text.util.Linkify.EMAIL_ADDRESSES;

import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;

import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.danielstone.materialaboutlibrary.ConvenienceBuilder;
import com.danielstone.materialaboutlibrary.MaterialAboutFragment;
import com.danielstone.materialaboutlibrary.holders.MaterialAboutItemViewHolder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutActionItem.MaterialAboutActionItemViewHolder;
import com.danielstone.materialaboutlibrary.items.MaterialAboutItem;
import com.danielstone.materialaboutlibrary.items.MaterialAboutTitleItem;
import com.danielstone.materialaboutlibrary.model.MaterialAboutCard;
import com.danielstone.materialaboutlibrary.model.MaterialAboutList;
import com.danielstone.materialaboutlibrary.util.DefaultViewTypeManager;
import com.danielstone.materialaboutlibrary.util.ViewTypeManager;
import com.osmino.sova.R;

public class AboutFragment extends MaterialAboutFragment {
    static class ClickableDefaultViewTypeManager extends DefaultViewTypeManager {

        @Override
        public void setupItem(final int itemType,
                              final MaterialAboutItemViewHolder holder,
                              final MaterialAboutItem item,
                              final Context context) {
            super.setupItem(itemType, holder, item, context);
            if (holder instanceof MaterialAboutActionItemViewHolder) {
                if (!((MaterialAboutActionItemViewHolder) holder).view.hasOnClickListeners()) {
                    ((MaterialAboutActionItemViewHolder) holder).text.setMovementMethod(LinkMovementMethod.getInstance());
                }
            }
        }
    }
    @NonNull
    @Override
    protected ViewTypeManager getViewTypeManager() {
        return new ClickableDefaultViewTypeManager();
    }

    @Override
    protected MaterialAboutList getMaterialAboutList(Context activityContext) {
        String versionName = "";

        PackageInfo pInfo = null;
        try {
            pInfo = activityContext.getPackageManager().getPackageInfo(activityContext.getPackageName(), 0);
            versionName = pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        MaterialAboutCard.Builder mainCardBuilder = new MaterialAboutCard.Builder();
        mainCardBuilder.addItem(new MaterialAboutTitleItem.Builder()
                .desc(R.string.sova_about_title)
                .icon(R.drawable.ic_logo)
                .build());
        mainCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.sova_about_descr)
                .showIcon(false)
                .build());

        MaterialAboutCard.Builder licenseCardBuilder = new MaterialAboutCard.Builder();
        licenseCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.sova_about_license)
                .showIcon(false)
                .build());

        MaterialAboutCard.Builder agreementCardBuilder = new MaterialAboutCard.Builder();
        agreementCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.about_app_agreement)
                .icon(R.drawable.ic_outline_description_24)
                .setOnClickAction( () -> {
                        final AlertDialog dialog =  new AlertDialog.Builder(activityContext)
                            .setTitle(R.string.about_app_agreement)
                            .setMessage(R.string.user_agreement_text)
                            .setNegativeButton("OK", (d, id) -> d.dismiss())
                            .create();
                        dialog.show();

                        View messageView = dialog.findViewById(android.R.id.message);
                        if (messageView instanceof TextView) {
                            TextView tv = (TextView)messageView;
                            tv.setAutoLinkMask(EMAIL_ADDRESSES);
                            tv.setText(tv.getText());
                        }
                    }).build()
                );
        MaterialAboutCard.Builder appCardBuilder = new MaterialAboutCard.Builder();
        appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text(R.string.about_app_version)
                .icon(R.drawable.ic_info)
                .subText(versionName)
                .build());
        appCardBuilder.addItem(new MaterialAboutActionItem.Builder()
                .text("Virtual Assistant, LLC")
                .icon(R.drawable.ic_tsdc_logo_24)
                .setOnClickAction(
                        ConvenienceBuilder.createWebsiteOnClickAction(
                                activityContext, Uri.parse("https://play.google.com/store/apps/developer?id=Ashmanov+Neural+Networks")))
                .build());
        appCardBuilder.addItem(ConvenienceBuilder.createEmailItem(activityContext,
                getResources().getDrawable(R.drawable.ic_outline_email_24, null),
                getString(R.string.about_app_email),
                true,
                getString(R.string.preference_support_email),
                "Feedback"));

        return new MaterialAboutList.Builder()
                    .addCard(mainCardBuilder.build())
                    .addCard(licenseCardBuilder.build())
                    .addCard(agreementCardBuilder.build())
                    .addCard(appCardBuilder.build())
                    .build();
    }
}
