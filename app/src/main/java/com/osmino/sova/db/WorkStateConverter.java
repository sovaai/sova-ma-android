package com.osmino.sova.db;

import androidx.room.TypeConverter;
import androidx.work.WorkInfo;
import androidx.work.WorkInfo.State;

public class WorkStateConverter {
    @TypeConverter
    public int toInt(WorkInfo.State state) {
        return state.ordinal();
    }

    @TypeConverter
    public WorkInfo.State toWorkState(int state) {
        final WorkInfo.State[] values = State.values();
        if (state < values.length) {
            return values[state];
        } else {
            return values[0];
        }
    }
}
