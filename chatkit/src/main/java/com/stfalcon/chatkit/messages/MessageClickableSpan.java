package com.stfalcon.chatkit.messages;

import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.view.View;

import androidx.annotation.NonNull;

public class MessageClickableSpan extends ClickableSpan {

    private MessageSpanListener listener;
    private String charSequence;
    private int linkColor;

    public MessageClickableSpan(String charSequence, MessageSpanListener listener, int linkColor) {
        this.charSequence = charSequence;
        this.listener = listener;
        this.linkColor = linkColor;
    }

    @Override
    public void onClick(@NonNull View view) {
        if(listener != null) {
            listener.onLinkClick(charSequence);
        }
    }

    @Override
    public void updateDrawState(@NonNull TextPaint ds) {
        super.updateDrawState(ds);

        ds.setUnderlineText(true);
        //ds.setColor(linkColor);
    }
}
