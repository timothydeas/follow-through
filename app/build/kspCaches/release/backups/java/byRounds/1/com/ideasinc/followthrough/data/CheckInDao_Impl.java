package com.ideasinc.followthrough.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import javax.annotation.processing.Generated;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlinx.coroutines.flow.Flow;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class CheckInDao_Impl implements CheckInDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<CheckIn> __insertionAdapterOfCheckIn;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public CheckInDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfCheckIn = new EntityInsertionAdapter<CheckIn>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `check_ins` (`id`,`goalId`,`goalOrChange`,`madeProgress`,`avoiding`,`confidence`,`competingPriority`,`implementationIntention`,`accountability`,`createdAt`,`updatedAt`) VALUES (?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final CheckIn entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getGoalId());
        statement.bindString(3, entity.getGoalOrChange());
        if (entity.getMadeProgress() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getMadeProgress());
        }
        if (entity.getAvoiding() == null) {
          statement.bindNull(5);
        } else {
          statement.bindString(5, entity.getAvoiding());
        }
        if (entity.getConfidence() == null) {
          statement.bindNull(6);
        } else {
          statement.bindString(6, entity.getConfidence());
        }
        if (entity.getCompetingPriority() == null) {
          statement.bindNull(7);
        } else {
          statement.bindString(7, entity.getCompetingPriority());
        }
        if (entity.getImplementationIntention() == null) {
          statement.bindNull(8);
        } else {
          statement.bindString(8, entity.getImplementationIntention());
        }
        if (entity.getAccountability() == null) {
          statement.bindNull(9);
        } else {
          statement.bindString(9, entity.getAccountability());
        }
        statement.bindLong(10, entity.getCreatedAt());
        statement.bindLong(11, entity.getUpdatedAt());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM check_ins WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertCheckIn(final CheckIn checkIn, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfCheckIn.insert(checkIn);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteById(final String id, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteById.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, id);
        try {
          __db.beginTransaction();
          try {
            _stmt.executeUpdateDelete();
            __db.setTransactionSuccessful();
            return Unit.INSTANCE;
          } finally {
            __db.endTransaction();
          }
        } finally {
          __preparedStmtOfDeleteById.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<CheckIn>> getAllCheckIns() {
    final String _sql = "SELECT * FROM check_ins ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"check_ins"}, new Callable<List<CheckIn>>() {
      @Override
      @NonNull
      public List<CheckIn> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "goalId");
          final int _cursorIndexOfGoalOrChange = CursorUtil.getColumnIndexOrThrow(_cursor, "goalOrChange");
          final int _cursorIndexOfMadeProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "madeProgress");
          final int _cursorIndexOfAvoiding = CursorUtil.getColumnIndexOrThrow(_cursor, "avoiding");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfCompetingPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "competingPriority");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final int _cursorIndexOfAccountability = CursorUtil.getColumnIndexOrThrow(_cursor, "accountability");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<CheckIn> _result = new ArrayList<CheckIn>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CheckIn _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpGoalId;
            _tmpGoalId = _cursor.getString(_cursorIndexOfGoalId);
            final String _tmpGoalOrChange;
            _tmpGoalOrChange = _cursor.getString(_cursorIndexOfGoalOrChange);
            final String _tmpMadeProgress;
            if (_cursor.isNull(_cursorIndexOfMadeProgress)) {
              _tmpMadeProgress = null;
            } else {
              _tmpMadeProgress = _cursor.getString(_cursorIndexOfMadeProgress);
            }
            final String _tmpAvoiding;
            if (_cursor.isNull(_cursorIndexOfAvoiding)) {
              _tmpAvoiding = null;
            } else {
              _tmpAvoiding = _cursor.getString(_cursorIndexOfAvoiding);
            }
            final String _tmpConfidence;
            if (_cursor.isNull(_cursorIndexOfConfidence)) {
              _tmpConfidence = null;
            } else {
              _tmpConfidence = _cursor.getString(_cursorIndexOfConfidence);
            }
            final String _tmpCompetingPriority;
            if (_cursor.isNull(_cursorIndexOfCompetingPriority)) {
              _tmpCompetingPriority = null;
            } else {
              _tmpCompetingPriority = _cursor.getString(_cursorIndexOfCompetingPriority);
            }
            final String _tmpImplementationIntention;
            if (_cursor.isNull(_cursorIndexOfImplementationIntention)) {
              _tmpImplementationIntention = null;
            } else {
              _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            }
            final String _tmpAccountability;
            if (_cursor.isNull(_cursorIndexOfAccountability)) {
              _tmpAccountability = null;
            } else {
              _tmpAccountability = _cursor.getString(_cursorIndexOfAccountability);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new CheckIn(_tmpId,_tmpGoalId,_tmpGoalOrChange,_tmpMadeProgress,_tmpAvoiding,_tmpConfidence,_tmpCompetingPriority,_tmpImplementationIntention,_tmpAccountability,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Flow<List<CheckIn>> getCheckInsForGoal(final String goalId) {
    final String _sql = "SELECT * FROM check_ins WHERE goalId = ? ORDER BY createdAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, goalId);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"check_ins"}, new Callable<List<CheckIn>>() {
      @Override
      @NonNull
      public List<CheckIn> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "goalId");
          final int _cursorIndexOfGoalOrChange = CursorUtil.getColumnIndexOrThrow(_cursor, "goalOrChange");
          final int _cursorIndexOfMadeProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "madeProgress");
          final int _cursorIndexOfAvoiding = CursorUtil.getColumnIndexOrThrow(_cursor, "avoiding");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfCompetingPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "competingPriority");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final int _cursorIndexOfAccountability = CursorUtil.getColumnIndexOrThrow(_cursor, "accountability");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final List<CheckIn> _result = new ArrayList<CheckIn>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final CheckIn _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpGoalId;
            _tmpGoalId = _cursor.getString(_cursorIndexOfGoalId);
            final String _tmpGoalOrChange;
            _tmpGoalOrChange = _cursor.getString(_cursorIndexOfGoalOrChange);
            final String _tmpMadeProgress;
            if (_cursor.isNull(_cursorIndexOfMadeProgress)) {
              _tmpMadeProgress = null;
            } else {
              _tmpMadeProgress = _cursor.getString(_cursorIndexOfMadeProgress);
            }
            final String _tmpAvoiding;
            if (_cursor.isNull(_cursorIndexOfAvoiding)) {
              _tmpAvoiding = null;
            } else {
              _tmpAvoiding = _cursor.getString(_cursorIndexOfAvoiding);
            }
            final String _tmpConfidence;
            if (_cursor.isNull(_cursorIndexOfConfidence)) {
              _tmpConfidence = null;
            } else {
              _tmpConfidence = _cursor.getString(_cursorIndexOfConfidence);
            }
            final String _tmpCompetingPriority;
            if (_cursor.isNull(_cursorIndexOfCompetingPriority)) {
              _tmpCompetingPriority = null;
            } else {
              _tmpCompetingPriority = _cursor.getString(_cursorIndexOfCompetingPriority);
            }
            final String _tmpImplementationIntention;
            if (_cursor.isNull(_cursorIndexOfImplementationIntention)) {
              _tmpImplementationIntention = null;
            } else {
              _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            }
            final String _tmpAccountability;
            if (_cursor.isNull(_cursorIndexOfAccountability)) {
              _tmpAccountability = null;
            } else {
              _tmpAccountability = _cursor.getString(_cursorIndexOfAccountability);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _item = new CheckIn(_tmpId,_tmpGoalId,_tmpGoalOrChange,_tmpMadeProgress,_tmpAvoiding,_tmpConfidence,_tmpCompetingPriority,_tmpImplementationIntention,_tmpAccountability,_tmpCreatedAt,_tmpUpdatedAt);
            _result.add(_item);
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @Override
  public Object getCheckInById(final String id, final Continuation<? super CheckIn> $completion) {
    final String _sql = "SELECT * FROM check_ins WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<CheckIn>() {
      @Override
      @Nullable
      public CheckIn call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "goalId");
          final int _cursorIndexOfGoalOrChange = CursorUtil.getColumnIndexOrThrow(_cursor, "goalOrChange");
          final int _cursorIndexOfMadeProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "madeProgress");
          final int _cursorIndexOfAvoiding = CursorUtil.getColumnIndexOrThrow(_cursor, "avoiding");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfCompetingPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "competingPriority");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final int _cursorIndexOfAccountability = CursorUtil.getColumnIndexOrThrow(_cursor, "accountability");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final CheckIn _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpGoalId;
            _tmpGoalId = _cursor.getString(_cursorIndexOfGoalId);
            final String _tmpGoalOrChange;
            _tmpGoalOrChange = _cursor.getString(_cursorIndexOfGoalOrChange);
            final String _tmpMadeProgress;
            if (_cursor.isNull(_cursorIndexOfMadeProgress)) {
              _tmpMadeProgress = null;
            } else {
              _tmpMadeProgress = _cursor.getString(_cursorIndexOfMadeProgress);
            }
            final String _tmpAvoiding;
            if (_cursor.isNull(_cursorIndexOfAvoiding)) {
              _tmpAvoiding = null;
            } else {
              _tmpAvoiding = _cursor.getString(_cursorIndexOfAvoiding);
            }
            final String _tmpConfidence;
            if (_cursor.isNull(_cursorIndexOfConfidence)) {
              _tmpConfidence = null;
            } else {
              _tmpConfidence = _cursor.getString(_cursorIndexOfConfidence);
            }
            final String _tmpCompetingPriority;
            if (_cursor.isNull(_cursorIndexOfCompetingPriority)) {
              _tmpCompetingPriority = null;
            } else {
              _tmpCompetingPriority = _cursor.getString(_cursorIndexOfCompetingPriority);
            }
            final String _tmpImplementationIntention;
            if (_cursor.isNull(_cursorIndexOfImplementationIntention)) {
              _tmpImplementationIntention = null;
            } else {
              _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            }
            final String _tmpAccountability;
            if (_cursor.isNull(_cursorIndexOfAccountability)) {
              _tmpAccountability = null;
            } else {
              _tmpAccountability = _cursor.getString(_cursorIndexOfAccountability);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new CheckIn(_tmpId,_tmpGoalId,_tmpGoalOrChange,_tmpMadeProgress,_tmpAvoiding,_tmpConfidence,_tmpCompetingPriority,_tmpImplementationIntention,_tmpAccountability,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
          _statement.release();
        }
      }
    }, $completion);
  }

  @Override
  public Flow<CheckIn> getCheckInByIdAsFlow(final String id) {
    final String _sql = "SELECT * FROM check_ins WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"check_ins"}, new Callable<CheckIn>() {
      @Override
      @Nullable
      public CheckIn call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfGoalId = CursorUtil.getColumnIndexOrThrow(_cursor, "goalId");
          final int _cursorIndexOfGoalOrChange = CursorUtil.getColumnIndexOrThrow(_cursor, "goalOrChange");
          final int _cursorIndexOfMadeProgress = CursorUtil.getColumnIndexOrThrow(_cursor, "madeProgress");
          final int _cursorIndexOfAvoiding = CursorUtil.getColumnIndexOrThrow(_cursor, "avoiding");
          final int _cursorIndexOfConfidence = CursorUtil.getColumnIndexOrThrow(_cursor, "confidence");
          final int _cursorIndexOfCompetingPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "competingPriority");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final int _cursorIndexOfAccountability = CursorUtil.getColumnIndexOrThrow(_cursor, "accountability");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final CheckIn _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpGoalId;
            _tmpGoalId = _cursor.getString(_cursorIndexOfGoalId);
            final String _tmpGoalOrChange;
            _tmpGoalOrChange = _cursor.getString(_cursorIndexOfGoalOrChange);
            final String _tmpMadeProgress;
            if (_cursor.isNull(_cursorIndexOfMadeProgress)) {
              _tmpMadeProgress = null;
            } else {
              _tmpMadeProgress = _cursor.getString(_cursorIndexOfMadeProgress);
            }
            final String _tmpAvoiding;
            if (_cursor.isNull(_cursorIndexOfAvoiding)) {
              _tmpAvoiding = null;
            } else {
              _tmpAvoiding = _cursor.getString(_cursorIndexOfAvoiding);
            }
            final String _tmpConfidence;
            if (_cursor.isNull(_cursorIndexOfConfidence)) {
              _tmpConfidence = null;
            } else {
              _tmpConfidence = _cursor.getString(_cursorIndexOfConfidence);
            }
            final String _tmpCompetingPriority;
            if (_cursor.isNull(_cursorIndexOfCompetingPriority)) {
              _tmpCompetingPriority = null;
            } else {
              _tmpCompetingPriority = _cursor.getString(_cursorIndexOfCompetingPriority);
            }
            final String _tmpImplementationIntention;
            if (_cursor.isNull(_cursorIndexOfImplementationIntention)) {
              _tmpImplementationIntention = null;
            } else {
              _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            }
            final String _tmpAccountability;
            if (_cursor.isNull(_cursorIndexOfAccountability)) {
              _tmpAccountability = null;
            } else {
              _tmpAccountability = _cursor.getString(_cursorIndexOfAccountability);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            _result = new CheckIn(_tmpId,_tmpGoalId,_tmpGoalOrChange,_tmpMadeProgress,_tmpAvoiding,_tmpConfidence,_tmpCompetingPriority,_tmpImplementationIntention,_tmpAccountability,_tmpCreatedAt,_tmpUpdatedAt);
          } else {
            _result = null;
          }
          return _result;
        } finally {
          _cursor.close();
        }
      }

      @Override
      protected void finalize() {
        _statement.release();
      }
    });
  }

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
