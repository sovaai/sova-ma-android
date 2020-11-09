package com.osmino.sova.model.chat;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;
import androidx.work.WorkInfo;
import com.osmino.sova.db.WorkStateConverter;
import com.osmino.sova.model.Assistant;
import com.stfalcon.chatkit.commons.models.IMessage;
import com.stfalcon.chatkit.commons.models.IUser;
import com.stfalcon.chatkit.messages.MessageSpanListener;
import java.util.Date;
import java.util.Objects;
import org.jetbrains.annotations.NotNull;

@Entity(tableName = "messages",
        foreignKeys = @ForeignKey(entity = Assistant.class,
                parentColumns = "id",
                childColumns = "assistantId",
                onDelete = ForeignKey.CASCADE),
        primaryKeys = {"id", "assistantId"},
        indices = {@Index("assistantId")})
@TypeConverters(WorkStateConverter.class)
public class Message implements IMessage {
    @NotNull
    private String id;

    private int assistantId;

    @ColumnInfo(name = "text")
    private String text;

    @ColumnInfo(name = "incoming")
    private boolean incoming;

    @Ignore
    private Date createdAt;

    @ColumnInfo(name = "emotionNumber")
    private int emotionNumber;

    @Ignore
    public WorkInfo.State voicingState = WorkInfo.State.ENQUEUED;

    @Ignore
    public String voicingWavPath = "";

    public Message(){
    }

    public Message(String id, int assistantId, String text, boolean isIncoming) {
        this(id, assistantId, text, isIncoming, new Date());
    }

    public Message(@NonNull String id, int assistantId, String text, boolean isIncoming, Date createdAt) {
        this.id = id;
        this.text = text;
        this.assistantId = assistantId;
        this.incoming = isIncoming;
        this.createdAt = createdAt;
    }

    public String getId() {
        return id;
    }
    public void  setId(String id) {
        this.id = id;
    }

    public boolean getIncoming() {
        return incoming;
    }
    public void setIncoming(boolean incoming) {
        this.incoming = incoming;

    }

    @Override
    public String toString() {
        return "Message{" +
                "text='" + text + '\'' +
                ", assistantId=" + assistantId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Message message = (Message) o;
        return Objects.equals(id, message.id) &&
                Objects.equals(text, message.text) &&
                Objects.equals(createdAt, message.createdAt) &&
                Objects.equals(incoming, message.incoming) &&
                Objects.equals(assistantId, message.assistantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, text, createdAt, assistantId, incoming);
    }

    public String getText() {
        return text;
    }

    @Ignore
    private MessageSpanListener listener;

    @Override
    public IUser getUser() {
        return null;
    }

    @Override
    public Date getCreatedAt() {
        return createdAt;
    }

    public void setText(String text) {
        this.text = text;
    }

    public int getEmotionNumber() {
        return emotionNumber;
    }
    public void setEmotionNumber(final int emotionNumber) {
        this.emotionNumber = emotionNumber;
    }

    public int getAssistantId() {
        return assistantId;
    }
    public void setAssistantId(final int assistantId) {
        this.assistantId = assistantId;
    }
}
