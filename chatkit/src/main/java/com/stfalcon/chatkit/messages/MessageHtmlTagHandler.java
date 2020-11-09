package com.stfalcon.chatkit.messages;

import android.text.Editable;
import android.text.Html;
import android.text.Spannable;
import android.util.Log;

import org.xml.sax.XMLReader;

public class MessageHtmlTagHandler implements Html.TagHandler {
    private final MessageSpanListener mListener;
    private final int mSpanColor;

    MessageHtmlTagHandler(MessageSpanListener listener, int spanColor){
        mListener = listener;
        mSpanColor = spanColor;
    }

    public void handleTag(boolean opening, String tag, Editable output,
                          XMLReader xmlReader) {
        if(tag.equalsIgnoreCase("userlink")) {
            processUserlink(opening, output);
        }
    }

    private void processUserlink(boolean opening, Editable output) {
        int len = output.length();
        if(opening) {
            output.setSpan(new MessageClickableSpan("", mListener, mSpanColor), len, len, Spannable.SPAN_MARK_MARK);
        } else {
            Object obj = getLast(output, MessageClickableSpan.class);
            int where = output.getSpanStart(obj);

            output.removeSpan(obj);

            if (where != len) {
                String linkText = output.subSequence(where, len).toString();
                Log.d("MessageHtmlTagHandler", "Add link (" + where + ", " + len +") to text '" + linkText + "'");
                output.setSpan(new MessageClickableSpan(linkText, mListener, mSpanColor), where, len, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            }
        }
    }

    private Object getLast(Editable text, Class kind) {
        Object[] objs = text.getSpans(0, text.length(), kind);

        if (objs.length != 0) {
            for (int i = objs.length; i > 0; i--) {
                if (text.getSpanFlags(objs[i - 1]) == Spannable.SPAN_MARK_MARK) {
                    return objs[i - 1];
                }
            }
        }
        return null;
    }


}
