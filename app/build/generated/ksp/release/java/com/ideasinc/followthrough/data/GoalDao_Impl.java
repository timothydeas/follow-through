package com.ideasinc.followthrough.data;

import android.database.Cursor;
import android.os.CancellationSignal;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.CoroutinesRoom;
import androidx.room.EntityDeletionOrUpdateAdapter;
import androidx.room.EntityInsertionAdapter;
import androidx.room.RoomDatabase;
import androidx.room.RoomSQLiteQuery;
import androidx.room.SharedSQLiteStatement;
import androidx.room.util.CursorUtil;
import androidx.room.util.DBUtil;
import androidx.sqlite.db.SupportSQLiteStatement;
import java.lang.Class;
import java.lang.Exception;
import java.lang.Integer;
import java.lang.Long;
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
public final class GoalDao_Impl implements GoalDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<Goal> __insertionAdapterOfGoal;

  private final EntityDeletionOrUpdateAdapter<Goal> __updateAdapterOfGoal;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public GoalDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGoal = new EntityInsertionAdapter<Goal>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `goals` (`id`,`title`,`accountableTo`,`createdAt`,`updatedAt`,`priority`,`followedThrough`,`followedThroughAt`) VALUES (?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Goal entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getAccountableTo() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAccountableTo());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getUpdatedAt());
        if (entity.getPriority() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getPriority());
        }
        final int _tmp = entity.getFollowedThrough() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getFollowedThroughAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getFollowedThroughAt());
        }
      }
    };
    this.__updateAdapterOfGoal = new EntityDeletionOrUpdateAdapter<Goal>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `goals` SET `id` = ?,`title` = ?,`accountableTo` = ?,`createdAt` = ?,`updatedAt` = ?,`priority` = ?,`followedThrough` = ?,`followedThroughAt` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final Goal entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        if (entity.getAccountableTo() == null) {
          statement.bindNull(3);
        } else {
          statement.bindString(3, entity.getAccountableTo());
        }
        statement.bindLong(4, entity.getCreatedAt());
        statement.bindLong(5, entity.getUpdatedAt());
        if (entity.getPriority() == null) {
          statement.bindNull(6);
        } else {
          statement.bindLong(6, entity.getPriority());
        }
        final int _tmp = entity.getFollowedThrough() ? 1 : 0;
        statement.bindLong(7, _tmp);
        if (entity.getFollowedThroughAt() == null) {
          statement.bindNull(8);
        } else {
          statement.bindLong(8, entity.getFollowedThroughAt());
        }
        statement.bindString(9, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM goals WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertGoal(final Goal goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGoal.insert(goal);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateGoal(final Goal goal, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfGoal.handle(goal);
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
  public Flow<List<Goal>> getAllGoals() {
    final String _sql = "SELECT * FROM goals ORDER BY updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<List<Goal>>() {
      @Override
      @NonNull
      public List<Goal> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAccountableTo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountableTo");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfFollowedThrough = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThrough");
          final int _cursorIndexOfFollowedThroughAt = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThroughAt");
          final List<Goal> _result = new ArrayList<Goal>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final Goal _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpAccountableTo;
            if (_cursor.isNull(_cursorIndexOfAccountableTo)) {
              _tmpAccountableTo = null;
            } else {
              _tmpAccountableTo = _cursor.getString(_cursorIndexOfAccountableTo);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Integer _tmpPriority;
            if (_cursor.isNull(_cursorIndexOfPriority)) {
              _tmpPriority = null;
            } else {
              _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            }
            final boolean _tmpFollowedThrough;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowedThrough);
            _tmpFollowedThrough = _tmp != 0;
            final Long _tmpFollowedThroughAt;
            if (_cursor.isNull(_cursorIndexOfFollowedThroughAt)) {
              _tmpFollowedThroughAt = null;
            } else {
              _tmpFollowedThroughAt = _cursor.getLong(_cursorIndexOfFollowedThroughAt);
            }
            _item = new Goal(_tmpId,_tmpTitle,_tmpAccountableTo,_tmpCreatedAt,_tmpUpdatedAt,_tmpPriority,_tmpFollowedThrough,_tmpFollowedThroughAt);
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
  public Object getGoalById(final String id, final Continuation<? super Goal> $completion) {
    final String _sql = "SELECT * FROM goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<Goal>() {
      @Override
      @Nullable
      public Goal call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAccountableTo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountableTo");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfFollowedThrough = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThrough");
          final int _cursorIndexOfFollowedThroughAt = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThroughAt");
          final Goal _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpAccountableTo;
            if (_cursor.isNull(_cursorIndexOfAccountableTo)) {
              _tmpAccountableTo = null;
            } else {
              _tmpAccountableTo = _cursor.getString(_cursorIndexOfAccountableTo);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Integer _tmpPriority;
            if (_cursor.isNull(_cursorIndexOfPriority)) {
              _tmpPriority = null;
            } else {
              _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            }
            final boolean _tmpFollowedThrough;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowedThrough);
            _tmpFollowedThrough = _tmp != 0;
            final Long _tmpFollowedThroughAt;
            if (_cursor.isNull(_cursorIndexOfFollowedThroughAt)) {
              _tmpFollowedThroughAt = null;
            } else {
              _tmpFollowedThroughAt = _cursor.getLong(_cursorIndexOfFollowedThroughAt);
            }
            _result = new Goal(_tmpId,_tmpTitle,_tmpAccountableTo,_tmpCreatedAt,_tmpUpdatedAt,_tmpPriority,_tmpFollowedThrough,_tmpFollowedThroughAt);
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
  public Flow<Goal> getGoalByIdAsFlow(final String id) {
    final String _sql = "SELECT * FROM goals WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"goals"}, new Callable<Goal>() {
      @Override
      @Nullable
      public Goal call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfAccountableTo = CursorUtil.getColumnIndexOrThrow(_cursor, "accountableTo");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfPriority = CursorUtil.getColumnIndexOrThrow(_cursor, "priority");
          final int _cursorIndexOfFollowedThrough = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThrough");
          final int _cursorIndexOfFollowedThroughAt = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThroughAt");
          final Goal _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpAccountableTo;
            if (_cursor.isNull(_cursorIndexOfAccountableTo)) {
              _tmpAccountableTo = null;
            } else {
              _tmpAccountableTo = _cursor.getString(_cursorIndexOfAccountableTo);
            }
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final Integer _tmpPriority;
            if (_cursor.isNull(_cursorIndexOfPriority)) {
              _tmpPriority = null;
            } else {
              _tmpPriority = _cursor.getInt(_cursorIndexOfPriority);
            }
            final boolean _tmpFollowedThrough;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfFollowedThrough);
            _tmpFollowedThrough = _tmp != 0;
            final Long _tmpFollowedThroughAt;
            if (_cursor.isNull(_cursorIndexOfFollowedThroughAt)) {
              _tmpFollowedThroughAt = null;
            } else {
              _tmpFollowedThroughAt = _cursor.getLong(_cursorIndexOfFollowedThroughAt);
            }
            _result = new Goal(_tmpId,_tmpTitle,_tmpAccountableTo,_tmpCreatedAt,_tmpUpdatedAt,_tmpPriority,_tmpFollowedThrough,_tmpFollowedThroughAt);
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
