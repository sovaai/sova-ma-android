package com.osmino.sova.ui;

import android.util.Log;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.paging.PagedList;
import androidx.paging.PagedListAdapter;
import androidx.recyclerview.widget.DiffUtil;

import com.osmino.sova.model.chat.Message;
import com.stfalcon.chatkit.commons.ViewHolder;
import com.stfalcon.chatkit.messages.MessageHolders;
import com.stfalcon.chatkit.messages.MessageSpanListener;
import com.stfalcon.chatkit.messages.MessagesListStyle;

public class ChatMessagesListAdapter extends PagedListAdapter<Message, ViewHolder> implements MessageSpanListener {
    private String TAG = "ChatMessagesListAdapter";
    private MessageHolders holders;
    private MessagesListStyle messagesListStyle;
    private MessageSpanListener listener;

    public ChatMessagesListAdapter(MessageSpanListener listener) {
        super(DIFF_CALLBACK);

        this.holders = new MessageHolders();
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return holders.getHolder(parent, viewType, messagesListStyle);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Message message = getItem(position);
        if (message == null){
            return;
        }

        holders.bind(holder,
                message,
                false,
                null,
                null,
                null,
                null,
                this);
    }

    @Override
    public int getItemViewType(int position) {
        Message message = getItem(position);
        if (message != null && message.getIncoming()) {
            return MessageHolders.VIEW_TYPE_TEXT_MESSAGE;
        } else {
            return -MessageHolders.VIEW_TYPE_TEXT_MESSAGE;
        }
    }

    @Override
    public void onCurrentListChanged(@Nullable PagedList<Message> previousList, @Nullable PagedList<Message> currentList) {
        super.onCurrentListChanged(previousList, currentList);


    }

    void setStyle(MessagesListStyle style) {
        this.messagesListStyle = style;
    }

    private static DiffUtil.ItemCallback<Message> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Message>() {

                @Override
                public boolean areItemsTheSame(Message oldItem, Message newItem) {
                    // The ID property identifies when items are the same.
                    return oldItem.getId().equals(newItem.getId());
                }

                @Override
                public boolean areContentsTheSame(Message oldItem, Message newItem) {
                    // Don't use the "==" operator here. Either implement and use .equals(),
                    // or write custom data comparison logic here.
                    return oldItem.equals(newItem);
                }
            };

    @Override
    public void onLinkClick(String charSequenceClicked) {
        if (listener != null) {
            listener.onLinkClick(charSequenceClicked);
        }
    }
}
