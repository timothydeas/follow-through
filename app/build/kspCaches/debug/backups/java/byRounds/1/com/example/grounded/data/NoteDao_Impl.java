package com.example.grounded.data;

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
public final class NoteDao_Impl implements NoteDao {
  private final RoomDatabase __db;

  private final EntityInsertionAdapter<GroundedNote> __insertionAdapterOfGroundedNote;

  private final Converters __converters = new Converters();

  private final EntityDeletionOrUpdateAdapter<GroundedNote> __updateAdapterOfGroundedNote;

  private final SharedSQLiteStatement __preparedStmtOfDeleteById;

  public NoteDao_Impl(@NonNull final RoomDatabase __db) {
    this.__db = __db;
    this.__insertionAdapterOfGroundedNote = new EntityInsertionAdapter<GroundedNote>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "INSERT OR REPLACE INTO `notes` (`id`,`title`,`body`,`tag`,`isPinned`,`createdAt`,`updatedAt`,`type`,`isDraft`,`reflection`,`whatStoppedYou`,`whatYouLearned`,`nextSteps`,`whenField`,`willField`,`followedThrough`,`followedThroughAt`,`implementationIntention`) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GroundedNote entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getBody());
        if (entity.getTag() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTag());
        }
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getUpdatedAt());
        final String _tmp_1 = __converters.fromNoteType(entity.getType());
        statement.bindString(8, _tmp_1);
        final int _tmp_2 = entity.isDraft() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        statement.bindString(10, entity.getReflection());
        statement.bindString(11, entity.getWhatStoppedYou());
        statement.bindString(12, entity.getWhatYouLearned());
        statement.bindString(13, entity.getNextSteps());
        statement.bindString(14, entity.getWhenField());
        statement.bindString(15, entity.getWillField());
        final int _tmp_3 = entity.getFollowedThrough() ? 1 : 0;
        statement.bindLong(16, _tmp_3);
        if (entity.getFollowedThroughAt() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getFollowedThroughAt());
        }
        statement.bindString(18, entity.getImplementationIntention());
      }
    };
    this.__updateAdapterOfGroundedNote = new EntityDeletionOrUpdateAdapter<GroundedNote>(__db) {
      @Override
      @NonNull
      protected String createQuery() {
        return "UPDATE OR ABORT `notes` SET `id` = ?,`title` = ?,`body` = ?,`tag` = ?,`isPinned` = ?,`createdAt` = ?,`updatedAt` = ?,`type` = ?,`isDraft` = ?,`reflection` = ?,`whatStoppedYou` = ?,`whatYouLearned` = ?,`nextSteps` = ?,`whenField` = ?,`willField` = ?,`followedThrough` = ?,`followedThroughAt` = ?,`implementationIntention` = ? WHERE `id` = ?";
      }

      @Override
      protected void bind(@NonNull final SupportSQLiteStatement statement,
          @NonNull final GroundedNote entity) {
        statement.bindString(1, entity.getId());
        statement.bindString(2, entity.getTitle());
        statement.bindString(3, entity.getBody());
        if (entity.getTag() == null) {
          statement.bindNull(4);
        } else {
          statement.bindString(4, entity.getTag());
        }
        final int _tmp = entity.isPinned() ? 1 : 0;
        statement.bindLong(5, _tmp);
        statement.bindLong(6, entity.getCreatedAt());
        statement.bindLong(7, entity.getUpdatedAt());
        final String _tmp_1 = __converters.fromNoteType(entity.getType());
        statement.bindString(8, _tmp_1);
        final int _tmp_2 = entity.isDraft() ? 1 : 0;
        statement.bindLong(9, _tmp_2);
        statement.bindString(10, entity.getReflection());
        statement.bindString(11, entity.getWhatStoppedYou());
        statement.bindString(12, entity.getWhatYouLearned());
        statement.bindString(13, entity.getNextSteps());
        statement.bindString(14, entity.getWhenField());
        statement.bindString(15, entity.getWillField());
        final int _tmp_3 = entity.getFollowedThrough() ? 1 : 0;
        statement.bindLong(16, _tmp_3);
        if (entity.getFollowedThroughAt() == null) {
          statement.bindNull(17);
        } else {
          statement.bindLong(17, entity.getFollowedThroughAt());
        }
        statement.bindString(18, entity.getImplementationIntention());
        statement.bindString(19, entity.getId());
      }
    };
    this.__preparedStmtOfDeleteById = new SharedSQLiteStatement(__db) {
      @Override
      @NonNull
      public String createQuery() {
        final String _query = "DELETE FROM notes WHERE id = ?";
        return _query;
      }
    };
  }

  @Override
  public Object insertNote(final GroundedNote note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __insertionAdapterOfGroundedNote.insert(note);
          __db.setTransactionSuccessful();
          return Unit.INSTANCE;
        } finally {
          __db.endTransaction();
        }
      }
    }, $completion);
  }

  @Override
  public Object updateNote(final GroundedNote note, final Continuation<? super Unit> $completion) {
    return CoroutinesRoom.execute(__db, true, new Callable<Unit>() {
      @Override
      @NonNull
      public Unit call() throws Exception {
        __db.beginTransaction();
        try {
          __updateAdapterOfGroundedNote.handle(note);
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
  public Flow<List<GroundedNote>> getAllNotes() {
    final String _sql = "SELECT * FROM notes ORDER BY isPinned DESC, updatedAt DESC";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 0);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<List<GroundedNote>>() {
      @Override
      @NonNull
      public List<GroundedNote> call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsDraft = CursorUtil.getColumnIndexOrThrow(_cursor, "isDraft");
          final int _cursorIndexOfReflection = CursorUtil.getColumnIndexOrThrow(_cursor, "reflection");
          final int _cursorIndexOfWhatStoppedYou = CursorUtil.getColumnIndexOrThrow(_cursor, "whatStoppedYou");
          final int _cursorIndexOfWhatYouLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "whatYouLearned");
          final int _cursorIndexOfNextSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "nextSteps");
          final int _cursorIndexOfWhenField = CursorUtil.getColumnIndexOrThrow(_cursor, "whenField");
          final int _cursorIndexOfWillField = CursorUtil.getColumnIndexOrThrow(_cursor, "willField");
          final int _cursorIndexOfFollowedThrough = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThrough");
          final int _cursorIndexOfFollowedThroughAt = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThroughAt");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final List<GroundedNote> _result = new ArrayList<GroundedNote>(_cursor.getCount());
          while (_cursor.moveToNext()) {
            final GroundedNote _item;
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final String _tmpTag;
            if (_cursor.isNull(_cursorIndexOfTag)) {
              _tmpTag = null;
            } else {
              _tmpTag = _cursor.getString(_cursorIndexOfTag);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final NoteType _tmpType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toNoteType(_tmp_1);
            final boolean _tmpIsDraft;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDraft);
            _tmpIsDraft = _tmp_2 != 0;
            final String _tmpReflection;
            _tmpReflection = _cursor.getString(_cursorIndexOfReflection);
            final String _tmpWhatStoppedYou;
            _tmpWhatStoppedYou = _cursor.getString(_cursorIndexOfWhatStoppedYou);
            final String _tmpWhatYouLearned;
            _tmpWhatYouLearned = _cursor.getString(_cursorIndexOfWhatYouLearned);
            final String _tmpNextSteps;
            _tmpNextSteps = _cursor.getString(_cursorIndexOfNextSteps);
            final String _tmpWhenField;
            _tmpWhenField = _cursor.getString(_cursorIndexOfWhenField);
            final String _tmpWillField;
            _tmpWillField = _cursor.getString(_cursorIndexOfWillField);
            final boolean _tmpFollowedThrough;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfFollowedThrough);
            _tmpFollowedThrough = _tmp_3 != 0;
            final Long _tmpFollowedThroughAt;
            if (_cursor.isNull(_cursorIndexOfFollowedThroughAt)) {
              _tmpFollowedThroughAt = null;
            } else {
              _tmpFollowedThroughAt = _cursor.getLong(_cursorIndexOfFollowedThroughAt);
            }
            final String _tmpImplementationIntention;
            _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            _item = new GroundedNote(_tmpId,_tmpTitle,_tmpBody,_tmpTag,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpType,_tmpIsDraft,_tmpReflection,_tmpWhatStoppedYou,_tmpWhatYouLearned,_tmpNextSteps,_tmpWhenField,_tmpWillField,_tmpFollowedThrough,_tmpFollowedThroughAt,_tmpImplementationIntention);
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
  public Object getNoteById(final String id, final Continuation<? super GroundedNote> $completion) {
    final String _sql = "SELECT * FROM notes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    final CancellationSignal _cancellationSignal = DBUtil.createCancellationSignal();
    return CoroutinesRoom.execute(__db, false, _cancellationSignal, new Callable<GroundedNote>() {
      @Override
      @Nullable
      public GroundedNote call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsDraft = CursorUtil.getColumnIndexOrThrow(_cursor, "isDraft");
          final int _cursorIndexOfReflection = CursorUtil.getColumnIndexOrThrow(_cursor, "reflection");
          final int _cursorIndexOfWhatStoppedYou = CursorUtil.getColumnIndexOrThrow(_cursor, "whatStoppedYou");
          final int _cursorIndexOfWhatYouLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "whatYouLearned");
          final int _cursorIndexOfNextSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "nextSteps");
          final int _cursorIndexOfWhenField = CursorUtil.getColumnIndexOrThrow(_cursor, "whenField");
          final int _cursorIndexOfWillField = CursorUtil.getColumnIndexOrThrow(_cursor, "willField");
          final int _cursorIndexOfFollowedThrough = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThrough");
          final int _cursorIndexOfFollowedThroughAt = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThroughAt");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final GroundedNote _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final String _tmpTag;
            if (_cursor.isNull(_cursorIndexOfTag)) {
              _tmpTag = null;
            } else {
              _tmpTag = _cursor.getString(_cursorIndexOfTag);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final NoteType _tmpType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toNoteType(_tmp_1);
            final boolean _tmpIsDraft;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDraft);
            _tmpIsDraft = _tmp_2 != 0;
            final String _tmpReflection;
            _tmpReflection = _cursor.getString(_cursorIndexOfReflection);
            final String _tmpWhatStoppedYou;
            _tmpWhatStoppedYou = _cursor.getString(_cursorIndexOfWhatStoppedYou);
            final String _tmpWhatYouLearned;
            _tmpWhatYouLearned = _cursor.getString(_cursorIndexOfWhatYouLearned);
            final String _tmpNextSteps;
            _tmpNextSteps = _cursor.getString(_cursorIndexOfNextSteps);
            final String _tmpWhenField;
            _tmpWhenField = _cursor.getString(_cursorIndexOfWhenField);
            final String _tmpWillField;
            _tmpWillField = _cursor.getString(_cursorIndexOfWillField);
            final boolean _tmpFollowedThrough;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfFollowedThrough);
            _tmpFollowedThrough = _tmp_3 != 0;
            final Long _tmpFollowedThroughAt;
            if (_cursor.isNull(_cursorIndexOfFollowedThroughAt)) {
              _tmpFollowedThroughAt = null;
            } else {
              _tmpFollowedThroughAt = _cursor.getLong(_cursorIndexOfFollowedThroughAt);
            }
            final String _tmpImplementationIntention;
            _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            _result = new GroundedNote(_tmpId,_tmpTitle,_tmpBody,_tmpTag,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpType,_tmpIsDraft,_tmpReflection,_tmpWhatStoppedYou,_tmpWhatYouLearned,_tmpNextSteps,_tmpWhenField,_tmpWillField,_tmpFollowedThrough,_tmpFollowedThroughAt,_tmpImplementationIntention);
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
  public Flow<GroundedNote> getNoteByIdAsFlow(final String id) {
    final String _sql = "SELECT * FROM notes WHERE id = ?";
    final RoomSQLiteQuery _statement = RoomSQLiteQuery.acquire(_sql, 1);
    int _argIndex = 1;
    _statement.bindString(_argIndex, id);
    return CoroutinesRoom.createFlow(__db, false, new String[] {"notes"}, new Callable<GroundedNote>() {
      @Override
      @Nullable
      public GroundedNote call() throws Exception {
        final Cursor _cursor = DBUtil.query(__db, _statement, false, null);
        try {
          final int _cursorIndexOfId = CursorUtil.getColumnIndexOrThrow(_cursor, "id");
          final int _cursorIndexOfTitle = CursorUtil.getColumnIndexOrThrow(_cursor, "title");
          final int _cursorIndexOfBody = CursorUtil.getColumnIndexOrThrow(_cursor, "body");
          final int _cursorIndexOfTag = CursorUtil.getColumnIndexOrThrow(_cursor, "tag");
          final int _cursorIndexOfIsPinned = CursorUtil.getColumnIndexOrThrow(_cursor, "isPinned");
          final int _cursorIndexOfCreatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "createdAt");
          final int _cursorIndexOfUpdatedAt = CursorUtil.getColumnIndexOrThrow(_cursor, "updatedAt");
          final int _cursorIndexOfType = CursorUtil.getColumnIndexOrThrow(_cursor, "type");
          final int _cursorIndexOfIsDraft = CursorUtil.getColumnIndexOrThrow(_cursor, "isDraft");
          final int _cursorIndexOfReflection = CursorUtil.getColumnIndexOrThrow(_cursor, "reflection");
          final int _cursorIndexOfWhatStoppedYou = CursorUtil.getColumnIndexOrThrow(_cursor, "whatStoppedYou");
          final int _cursorIndexOfWhatYouLearned = CursorUtil.getColumnIndexOrThrow(_cursor, "whatYouLearned");
          final int _cursorIndexOfNextSteps = CursorUtil.getColumnIndexOrThrow(_cursor, "nextSteps");
          final int _cursorIndexOfWhenField = CursorUtil.getColumnIndexOrThrow(_cursor, "whenField");
          final int _cursorIndexOfWillField = CursorUtil.getColumnIndexOrThrow(_cursor, "willField");
          final int _cursorIndexOfFollowedThrough = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThrough");
          final int _cursorIndexOfFollowedThroughAt = CursorUtil.getColumnIndexOrThrow(_cursor, "followedThroughAt");
          final int _cursorIndexOfImplementationIntention = CursorUtil.getColumnIndexOrThrow(_cursor, "implementationIntention");
          final GroundedNote _result;
          if (_cursor.moveToFirst()) {
            final String _tmpId;
            _tmpId = _cursor.getString(_cursorIndexOfId);
            final String _tmpTitle;
            _tmpTitle = _cursor.getString(_cursorIndexOfTitle);
            final String _tmpBody;
            _tmpBody = _cursor.getString(_cursorIndexOfBody);
            final String _tmpTag;
            if (_cursor.isNull(_cursorIndexOfTag)) {
              _tmpTag = null;
            } else {
              _tmpTag = _cursor.getString(_cursorIndexOfTag);
            }
            final boolean _tmpIsPinned;
            final int _tmp;
            _tmp = _cursor.getInt(_cursorIndexOfIsPinned);
            _tmpIsPinned = _tmp != 0;
            final long _tmpCreatedAt;
            _tmpCreatedAt = _cursor.getLong(_cursorIndexOfCreatedAt);
            final long _tmpUpdatedAt;
            _tmpUpdatedAt = _cursor.getLong(_cursorIndexOfUpdatedAt);
            final NoteType _tmpType;
            final String _tmp_1;
            _tmp_1 = _cursor.getString(_cursorIndexOfType);
            _tmpType = __converters.toNoteType(_tmp_1);
            final boolean _tmpIsDraft;
            final int _tmp_2;
            _tmp_2 = _cursor.getInt(_cursorIndexOfIsDraft);
            _tmpIsDraft = _tmp_2 != 0;
            final String _tmpReflection;
            _tmpReflection = _cursor.getString(_cursorIndexOfReflection);
            final String _tmpWhatStoppedYou;
            _tmpWhatStoppedYou = _cursor.getString(_cursorIndexOfWhatStoppedYou);
            final String _tmpWhatYouLearned;
            _tmpWhatYouLearned = _cursor.getString(_cursorIndexOfWhatYouLearned);
            final String _tmpNextSteps;
            _tmpNextSteps = _cursor.getString(_cursorIndexOfNextSteps);
            final String _tmpWhenField;
            _tmpWhenField = _cursor.getString(_cursorIndexOfWhenField);
            final String _tmpWillField;
            _tmpWillField = _cursor.getString(_cursorIndexOfWillField);
            final boolean _tmpFollowedThrough;
            final int _tmp_3;
            _tmp_3 = _cursor.getInt(_cursorIndexOfFollowedThrough);
            _tmpFollowedThrough = _tmp_3 != 0;
            final Long _tmpFollowedThroughAt;
            if (_cursor.isNull(_cursorIndexOfFollowedThroughAt)) {
              _tmpFollowedThroughAt = null;
            } else {
              _tmpFollowedThroughAt = _cursor.getLong(_cursorIndexOfFollowedThroughAt);
            }
            final String _tmpImplementationIntention;
            _tmpImplementationIntention = _cursor.getString(_cursorIndexOfImplementationIntention);
            _result = new GroundedNote(_tmpId,_tmpTitle,_tmpBody,_tmpTag,_tmpIsPinned,_tmpCreatedAt,_tmpUpdatedAt,_tmpType,_tmpIsDraft,_tmpReflection,_tmpWhatStoppedYou,_tmpWhatYouLearned,_tmpNextSteps,_tmpWhenField,_tmpWillField,_tmpFollowedThrough,_tmpFollowedThroughAt,_tmpImplementationIntention);
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
