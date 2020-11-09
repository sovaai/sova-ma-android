package com.osmino.sova.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.osmino.sova.model.chat.Message;
import com.stfalcon.chatkit.messages.MessageSpanListener;
import com.stfalcon.chatkit.messages.MessagesListStyle;

public class ChatMessagesList extends RecyclerView implements MessageSpanListener {
    private MessagesListStyle messagesListStyle;
    private ChatMessagesListAdapter messagesListAdapter;
    private MessageSpanListener linkClickListener;

    public ChatMessagesList(@NonNull Context context) {
        this(context, null);
    }

    public ChatMessagesList(@NonNull Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ChatMessagesList(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        messagesListStyle = MessagesListStyle.parse(context, attrs);

        LinearLayoutManager layoutManager = new LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false);
        layoutManager.setStackFromEnd(true);
        setLayoutManager(layoutManager);

        messagesListAdapter = new ChatMessagesListAdapter(this);
        messagesListAdapter.setStyle(messagesListStyle);
        messagesListAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                Log.d("ChatMessagesList", "onItemRangeInserted(" + positionStart + ", " + itemCount + ") " + messagesListAdapter.getItemCount());
                if (itemCount > 3) {
                    scrollToBottom();
                } else {
                    smoothScrollToPosition(messagesListAdapter.getItemCount());
                }
            }
        });

        setAdapter(messagesListAdapter);

        addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if (bottom < oldBottom) {
                scrollToBottom();
            }
        });
    }

    @Override
    protected void onMeasure(int widthSpec, int heightSpec) {
        super.onMeasure(widthSpec, heightSpec);

        //smoothScrollToPosition(messagesListAdapter.getItemCount());
    }

    public void submitList(@Nullable PagedList<Message> pagedList) {
        messagesListAdapter.submitList(pagedList);
    }

    @Nullable
    @Override
    public ChatMessagesListAdapter getAdapter() {
        Adapter adapter = super.getAdapter();
        if (adapter instanceof ChatMessagesListAdapter) {
            return (ChatMessagesListAdapter)adapter;
        }
        return null;
    }

    public void setLinkClickListener(MessageSpanListener listener) {
        this.linkClickListener = listener;
    }

    @Override
    public void onLinkClick(String charSequenceClicked) {
        if (linkClickListener != null) {
            linkClickListener.onLinkClick(charSequenceClicked);
        }
    }

    public void scrollToBottom() {
        // scroll to last item to get the view of last item
        final LinearLayoutManager layoutManager = (LinearLayoutManager)getLayoutManager();
        final RecyclerView.Adapter adapter = getAdapter();
        if (adapter == null || layoutManager == null) {
            return;
        }
        final int lastItemPosition = adapter.getItemCount() - 1;

        layoutManager.scrollToPositionWithOffset(lastItemPosition, 0);
        /*this.post(() -> {
            // then scroll to specific offset
            View target = layoutManager.findViewByPosition(lastItemPosition);
            if (target != null) {
                int offset = this.getMeasuredHeight() - target.getMeasuredHeight();
                layoutManager.scrollToPositionWithOffset(lastItemPosition, offset);
            } else {
                Log.e("ChatMessagesList", "Can`t find view by position");
            }
        });*/
    }
}
