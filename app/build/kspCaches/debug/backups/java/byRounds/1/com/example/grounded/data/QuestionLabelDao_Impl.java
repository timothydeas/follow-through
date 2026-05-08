package com.example.grounded.data;

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
public final class QuestionLabelDao_Impl implements QuestionLabelDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<QuestionLabel> __insertionAdapterOfQuestionLabel;

  private final SharedSQLiteStatement __preparedStmtOfDeleteByKey;

  public QuestionLabelDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfQuestionLabel = new EntityInsertionAdapter<QuestionLabel>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `question_labels` (`id`,`questionKey`,`customLabel`,`isEnabled`) VALUES (?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final QuestionLabel entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getQuestionKey());
        statement.bindString(3, entity.getCustomLabel());
        final int _tmp = entity.isEnabled() ? 1 : 0;
        statement.bindLong(4, _tmp);
      }
    };
    this.__preparedStmtOfDeleteByKey = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM question_labels WHERE questionKey = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertLabel(final QuestionLabel label,
      final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfQuestionLabel.insert(label);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object deleteByKey(final String key, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        final SupportSQLiteStatement _stmt = __preparedStmtOfDeleteByKey.acquire();
        int _argIndex = 1;
        _stmt.bindString(_argIndex, key);
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
          __preparedStmtOfDeleteByKey.release(_stmt);
        }
      }
    }, $completion);
  }

  @Override
  public Flow<List<QuestionLabel>> getAllLabels() {
    final String _sql = "SELECT * FROM question_labels";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"question_labels"}, new Callable<List<QuestionLabel>>() {
      @Override
      @NonNull
      public List<QuestionLabel> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQuestionKey = CursorUtil.getColumnIndexOrThrow(_cursor, "questionKey");
          final int _cursorIndexOfCustomLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "customLabel");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final List<QuestionLabel> _result = new ArrayList<QuestionLabel>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final QuestionLabel _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpQuestionKey;
            _tmpQuestionKey = _cursor.getString(_cursorIndexOfQuestionKey);
            final String _tmpCustomLabel;
            _tmpCustomLabel = _cursor.getString(_cursorIndexOfCustomLabel);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            _item = new QuestionLabel(_tmpId,_tmpQuestionKey,_tmpCustomLabel,_tmpIsEnabled);
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
  public Object getLabelForKey(final String key,
      final Continuation<? super QuestionLabel> $completion) {
    final String _sql = "SELECT * FROM question_labels WHERE questionKey = ? LIMIT 1";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, key);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<QuestionLabel>() {
      @Override
      @Nullable
      public QuestionLabel call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfQuestionKey = CursorUtil.getColumnIndexOrThrow(_cursor, "questionKey");
          final int _cursorIndexOfCustomLabel = CursorUtil.getColumnIndexOrThrow(_cursor, "customLabel");
          final int _cursorIndexOfIsEnabled = CursorUtil.getColumnIndexOrThrow(_cursor, "isEnabled");
          final QuestionLabel _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpQuestionKey;
            _tmpQuestionKey = _cursor.getString(_cursorIndexOfQuestionKey);
            final String _tmpCustomLabel;
            _tmpCustomLabel = _cursor.getString(_cursorIndexOfCustomLabel);
            final boolean _tmpIsEnabled;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsEnabled);
            _tmpIsEnabled = _tmp != 0;
            _result = new QuestionLabel(_tmpId,_tmpQuestionKey,_tmpCustomLabel,_tmpIsEnabled);
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

  @NonNull
  public static List<Class<?>> getRequiredConverters() {
    return Collections.emptyList();
  }
}
