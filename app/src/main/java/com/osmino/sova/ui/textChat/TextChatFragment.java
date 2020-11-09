package com.osmino.sova.ui.textChat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.paging.PagedList;

import com.osmino.sova.DataController;
import com.osmino.sova.R;
import com.osmino.sova.model.AssistantController;
import com.osmino.sova.model.chat.Message;
import com.osmino.sova.ui.ChatMessagesList;
import com.osmino.sova.ui.ChatMessagesListAdapter;

import java.util.Objects;

import static com.osmino.sova.DataController.ASSISTANT_CONTROLLER_CHANGED;

public class TextChatFragment extends Fragment {
    final String TAG = "TextChatFragment";

    private TextChatViewModel textChatViewModel;
    private DataController dc;
    private ChatMessagesList messagesList;
    private Parcelable lastLayoutManagerState;
    private LiveData<PagedList<Message>> assistantMessages = null;

    BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ASSISTANT_CONTROLLER_CHANGED.equals(intent.getAction())) {
                Log.d(TAG, "Assistant controller changed, new assistant id=" + dc.getAssistantController().getId());
                removeObserver();
                observeList(textChatViewModel.getLastKey());
            }
        }
    };

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        textChatViewModel = new ViewModelProvider(this).get(TextChatViewModel.class);
        dc = DataController.getInstance(requireActivity().getApplicationContext());
        View root = inflater.inflate(R.layout.fragment_textchat, container, false);

        messagesList = root.findViewById(R.id.messagesListNew);
        messagesList.setLinkClickListener((words) -> {
            if (dc.getAssistantController() != null) {
                Log.d(TAG, "Click on '" + words + "', send request");
                dc.getAssistantController().sendRequest(words);
            }
        });

        IntentFilter filter = new IntentFilter();
        filter.addAction(ASSISTANT_CONTROLLER_CHANGED);
        requireContext().registerReceiver(broadcastReceiver, filter);

        return root;
    }


    @Override
    public void onDestroy() {
        requireContext().unregisterReceiver(broadcastReceiver);
        removeObserver();
        super.onDestroy();
    }

    private void removeObserver() {
        if (assistantMessages != null) {
            assistantMessages.removeObserver(messagesObserver);
            assistantMessages = null;
        }
    }

    private int getLastKey() {
        ChatMessagesListAdapter adapter = messagesList.getAdapter();
        if (adapter != null) {
            PagedList<Message> currentList = adapter.getCurrentList();
            if (currentList != null) {
                return (int)currentList.getLastKey();
            }
        }
        return 0;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("lastKey", getLastKey());
        outState.putParcelable("layout_manager_state", Objects.requireNonNull(messagesList.getLayoutManager()).onSaveInstanceState());
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        int lastKey = textChatViewModel.getLastKey();

        Log.d(TAG, "Restored lastkey=" + lastKey);

        if (savedInstanceState != null) {
            lastLayoutManagerState = savedInstanceState.getParcelable("layout_manager_state");
            lastKey = savedInstanceState.getInt("lastKey");
            Log.d(TAG, "Saved state lastKey: " + lastKey);
        }

        observeList(lastKey);
    }

    Observer<PagedList<Message>> messagesObserver = new Observer<PagedList<Message>>() {
        @Override
        public void onChanged(final PagedList<Message> messages) {
            Log.d(TAG, "submit PagedList with size=" + messages.size());
            messagesList.submitList(messages);

            if (lastLayoutManagerState != null) {
                Objects.requireNonNull(messagesList.getLayoutManager()).onRestoreInstanceState(lastLayoutManagerState);
                lastLayoutManagerState = null;
            }
        }
    };

    private void observeList(int lastKey) {
        AssistantController assistantController = dc.getAssistantController();
        if (assistantController != null) {
            Log.d(TAG, "Observe assistant(" + assistantController.getId() + ") messages");
            assistantMessages = assistantController.getMessages(lastKey);
            assistantMessages.observe(getViewLifecycleOwner(), messagesObserver);
        } else {
            Log.d(TAG, "Assistant is null");
        }
    }
}
