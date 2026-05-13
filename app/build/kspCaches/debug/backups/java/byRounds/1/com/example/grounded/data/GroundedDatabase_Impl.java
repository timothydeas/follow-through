package com.example.grounded.data;

import androidx.annotation.NonNull;
import androidx.room.DatabaseConfiguration;
import androidx.room.InvalidationTracker;
import androidx.room.RoomDatabase;
import androidx.room.RoomOpenHelper;
import androidx.room.migration.AutoMigrationSpec;
import androidx.room.migration.Migration;
import androidx.room.util.DBUtil;
import androidx.room.util.TableInfo;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.sqlite.db.SupportSQLiteOpenHelper;
import java.lang.Class;
import java.lang.Override;
import java.lang.String;
import java.lang.SuppressWarnings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.processing.Generated;

@Generated("androidx.room.RoomProcessor")
@SuppressWarnings({"unchecked", "deprecation"})
public final class GroundedDatabase_Impl extends GroundedDatabase {
  private volatile NoteDao _noteDao;

  private volatile FollowThroughDao _followThroughDao;

  private volatile GoalDao _goalDao;

  private volatile CheckInDao _checkInDao;

  private volatile QuestionLabelDao _questionLabelDao;

  @Override
  @NonNull
  protected SupportSQLiteOpenHelper createOpenHelper(@NonNull final DatabaseConfiguration config) {
    final SupportSQLiteOpenHelper.Callback _openCallback = new RoomOpenHelper(config, new RoomOpenHelper.Delegate(20) {
      @Override
      public void createAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `notes` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `body` TEXT NOT NULL, `tag` TEXT, `isPinned` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `type` TEXT NOT NULL, `isDraft` INTEGER NOT NULL, `reflection` TEXT NOT NULL, `whatStoppedYou` TEXT NOT NULL, `whatYouLearned` TEXT NOT NULL, `nextSteps` TEXT NOT NULL, `whenField` TEXT NOT NULL, `willField` TEXT NOT NULL, `followedThrough` INTEGER NOT NULL, `followedThroughAt` INTEGER, `implementationIntention` TEXT NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `follow_throughs` (`id` TEXT NOT NULL, `noteId` TEXT NOT NULL, `body` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`noteId`) REFERENCES `notes`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_follow_throughs_noteId` ON `follow_throughs` (`noteId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `goals` (`id` TEXT NOT NULL, `title` TEXT NOT NULL, `accountableTo` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, `priority` INTEGER, `followedThrough` INTEGER NOT NULL, `followedThroughAt` INTEGER, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS `check_ins` (`id` TEXT NOT NULL, `goalId` TEXT NOT NULL, `goalOrChange` TEXT NOT NULL, `madeProgress` TEXT, `avoiding` TEXT, `confidence` TEXT, `competingPriority` TEXT, `implementationIntention` TEXT, `accountability` TEXT, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL, PRIMARY KEY(`id`), FOREIGN KEY(`goalId`) REFERENCES `goals`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE )");
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_check_ins_goalId` ON `check_ins` (`goalId`)");
        db.execSQL("CREATE TABLE IF NOT EXISTS `question_labels` (`id` TEXT NOT NULL, `questionKey` TEXT NOT NULL, `customLabel` TEXT NOT NULL, `isEnabled` INTEGER NOT NULL, PRIMARY KEY(`id`))");
        db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)");
        db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e47efc6cedc7b86904ab024e2f520db6')");
      }

      @Override
      public void dropAllTables(@NonNull final SupportSQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS `notes`");
        db.execSQL("DROP TABLE IF EXISTS `follow_throughs`");
        db.execSQL("DROP TABLE IF EXISTS `goals`");
        db.execSQL("DROP TABLE IF EXISTS `check_ins`");
        db.execSQL("DROP TABLE IF EXISTS `question_labels`");
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onDestructiveMigration(db);
          }
        }
      }

      @Override
      public void onCreate(@NonNull final SupportSQLiteDatabase db) {
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onCreate(db);
          }
        }
      }

      @Override
      public void onOpen(@NonNull final SupportSQLiteDatabase db) {
        mDatabase = db;
        db.execSQL("PRAGMA foreign_keys = ON");
        internalInitInvalidationTracker(db);
        final List<? extends RoomDatabase.Callback> _callbacks = mCallbacks;
        if (_callbacks != null) {
          for (RoomDatabase.Callback _callback : _callbacks) {
            _callback.onOpen(db);
          }
        }
      }

      @Override
      public void onPreMigrate(@NonNull final SupportSQLiteDatabase db) {
        DBUtil.dropFtsSyncTriggers(db);
      }

      @Override
      public void onPostMigrate(@NonNull final SupportSQLiteDatabase db) {
      }

      @Override
      @NonNull
      public RoomOpenHelper.ValidationResult onValidateSchema(
          @NonNull final SupportSQLiteDatabase db) {
        final HashMap<String, TableInfo.Column> _columnsNotes = new HashMap<String, TableInfo.Column>(18);
        _columnsNotes.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("body", new TableInfo.Column("body", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("tag", new TableInfo.Column("tag", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("isPinned", new TableInfo.Column("isPinned", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("type", new TableInfo.Column("type", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("isDraft", new TableInfo.Column("isDraft", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("reflection", new TableInfo.Column("reflection", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("whatStoppedYou", new TableInfo.Column("whatStoppedYou", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("whatYouLearned", new TableInfo.Column("whatYouLearned", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("nextSteps", new TableInfo.Column("nextSteps", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("whenField", new TableInfo.Column("whenField", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("willField", new TableInfo.Column("willField", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("followedThrough", new TableInfo.Column("followedThrough", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("followedThroughAt", new TableInfo.Column("followedThroughAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsNotes.put("implementationIntention", new TableInfo.Column("implementationIntention", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysNotes = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesNotes = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoNotes = new TableInfo("notes", _columnsNotes, _foreignKeysNotes, _indicesNotes);
        final TableInfo _existingNotes = TableInfo.read(db, "notes");
        if (!_infoNotes.equals(_existingNotes)) {
          return new RoomOpenHelper.ValidationResult(false, "notes(com.example.grounded.data.GroundedNote).\n"
                  + " Expected:\n" + _infoNotes + "\n"
                  + " Found:\n" + _existingNotes);
        }
        final HashMap<String, TableInfo.Column> _columnsFollowThroughs = new HashMap<String, TableInfo.Column>(4);
        _columnsFollowThroughs.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowThroughs.put("noteId", new TableInfo.Column("noteId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowThroughs.put("body", new TableInfo.Column("body", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsFollowThroughs.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysFollowThroughs = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysFollowThroughs.add(new TableInfo.ForeignKey("notes", "CASCADE", "NO ACTION", Arrays.asList("noteId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesFollowThroughs = new HashSet<TableInfo.Index>(1);
        _indicesFollowThroughs.add(new TableInfo.Index("index_follow_throughs_noteId", false, Arrays.asList("noteId"), Arrays.asList("ASC")));
        final TableInfo _infoFollowThroughs = new TableInfo("follow_throughs", _columnsFollowThroughs, _foreignKeysFollowThroughs, _indicesFollowThroughs);
        final TableInfo _existingFollowThroughs = TableInfo.read(db, "follow_throughs");
        if (!_infoFollowThroughs.equals(_existingFollowThroughs)) {
          return new RoomOpenHelper.ValidationResult(false, "follow_throughs(com.example.grounded.data.FollowThroughEntry).\n"
                  + " Expected:\n" + _infoFollowThroughs + "\n"
                  + " Found:\n" + _existingFollowThroughs);
        }
        final HashMap<String, TableInfo.Column> _columnsGoals = new HashMap<String, TableInfo.Column>(8);
        _columnsGoals.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("title", new TableInfo.Column("title", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("accountableTo", new TableInfo.Column("accountableTo", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("priority", new TableInfo.Column("priority", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("followedThrough", new TableInfo.Column("followedThrough", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsGoals.put("followedThroughAt", new TableInfo.Column("followedThroughAt", "INTEGER", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysGoals = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesGoals = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoGoals = new TableInfo("goals", _columnsGoals, _foreignKeysGoals, _indicesGoals);
        final TableInfo _existingGoals = TableInfo.read(db, "goals");
        if (!_infoGoals.equals(_existingGoals)) {
          return new RoomOpenHelper.ValidationResult(false, "goals(com.example.grounded.data.Goal).\n"
                  + " Expected:\n" + _infoGoals + "\n"
                  + " Found:\n" + _existingGoals);
        }
        final HashMap<String, TableInfo.Column> _columnsCheckIns = new HashMap<String, TableInfo.Column>(11);
        _columnsCheckIns.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("goalId", new TableInfo.Column("goalId", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("goalOrChange", new TableInfo.Column("goalOrChange", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("madeProgress", new TableInfo.Column("madeProgress", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("avoiding", new TableInfo.Column("avoiding", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("confidence", new TableInfo.Column("confidence", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("competingPriority", new TableInfo.Column("competingPriority", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("implementationIntention", new TableInfo.Column("implementationIntention", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("accountability", new TableInfo.Column("accountability", "TEXT", false, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("createdAt", new TableInfo.Column("createdAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsCheckIns.put("updatedAt", new TableInfo.Column("updatedAt", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysCheckIns = new HashSet<TableInfo.ForeignKey>(1);
        _foreignKeysCheckIns.add(new TableInfo.ForeignKey("goals", "CASCADE", "NO ACTION", Arrays.asList("goalId"), Arrays.asList("id")));
        final HashSet<TableInfo.Index> _indicesCheckIns = new HashSet<TableInfo.Index>(1);
        _indicesCheckIns.add(new TableInfo.Index("index_check_ins_goalId", false, Arrays.asList("goalId"), Arrays.asList("ASC")));
        final TableInfo _infoCheckIns = new TableInfo("check_ins", _columnsCheckIns, _foreignKeysCheckIns, _indicesCheckIns);
        final TableInfo _existingCheckIns = TableInfo.read(db, "check_ins");
        if (!_infoCheckIns.equals(_existingCheckIns)) {
          return new RoomOpenHelper.ValidationResult(false, "check_ins(com.example.grounded.data.CheckIn).\n"
                  + " Expected:\n" + _infoCheckIns + "\n"
                  + " Found:\n" + _existingCheckIns);
        }
        final HashMap<String, TableInfo.Column> _columnsQuestionLabels = new HashMap<String, TableInfo.Column>(4);
        _columnsQuestionLabels.put("id", new TableInfo.Column("id", "TEXT", true, 1, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestionLabels.put("questionKey", new TableInfo.Column("questionKey", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestionLabels.put("customLabel", new TableInfo.Column("customLabel", "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        _columnsQuestionLabels.put("isEnabled", new TableInfo.Column("isEnabled", "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY));
        final HashSet<TableInfo.ForeignKey> _foreignKeysQuestionLabels = new HashSet<TableInfo.ForeignKey>(0);
        final HashSet<TableInfo.Index> _indicesQuestionLabels = new HashSet<TableInfo.Index>(0);
        final TableInfo _infoQuestionLabels = new TableInfo("question_labels", _columnsQuestionLabels, _foreignKeysQuestionLabels, _indicesQuestionLabels);
        final TableInfo _existingQuestionLabels = TableInfo.read(db, "question_labels");
        if (!_infoQuestionLabels.equals(_existingQuestionLabels)) {
          return new RoomOpenHelper.ValidationResult(false, "question_labels(com.example.grounded.data.QuestionLabel).\n"
                  + " Expected:\n" + _infoQuestionLabels + "\n"
                  + " Found:\n" + _existingQuestionLabels);
        }
        return new RoomOpenHelper.ValidationResult(true, null);
      }
    }, "e47efc6cedc7b86904ab024e2f520db6", "c055b467f05669f833878eeb921e74fc");
    final SupportSQLiteOpenHelper.Configuration _sqliteConfig = SupportSQLiteOpenHelper.Configuration.builder(config.context).name(config.name).callback(_openCallback).build();
    final SupportSQLiteOpenHelper _helper = config.sqliteOpenHelperFactory.create(_sqliteConfig);
    return _helper;
  }

  @Override
  @NonNull
  protected InvalidationTracker createInvalidationTracker() {
    final HashMap<String, String> _shadowTablesMap = new HashMap<String, String>(0);
    final HashMap<String, Set<String>> _viewTables = new HashMap<String, Set<String>>(0);
    return new InvalidationTracker(this, _shadowTablesMap, _viewTables, "notes","follow_throughs","goals","check_ins","question_labels");
  }

  @Override
  public void clearAllTables() {
    super.assertNotMainThread();
    final SupportSQLiteDatabase _db = super.getOpenHelper().getWritableDatabase();
    final boolean _supportsDeferForeignKeys = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP;
    try {
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = FALSE");
      }
      super.beginTransaction();
      if (_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA defer_foreign_keys = TRUE");
      }
      _db.execSQL("DELETE FROM `notes`");
      _db.execSQL("DELETE FROM `follow_throughs`");
      _db.execSQL("DELETE FROM `goals`");
      _db.execSQL("DELETE FROM `check_ins`");
      _db.execSQL("DELETE FROM `question_labels`");
      super.setTransactionSuccessful();
    } finally {
      super.endTransaction();
      if (!_supportsDeferForeignKeys) {
        _db.execSQL("PRAGMA foreign_keys = TRUE");
      }
      _db.query("PRAGMA wal_checkpoint(FULL)").close();
      if (!_db.inTransaction()) {
        _db.execSQL("VACUUM");
      }
    }
  }

  @Override
  @NonNull
  protected Map<Class<?>, List<Class<?>>> getRequiredTypeConverters() {
    final HashMap<Class<?>, List<Class<?>>> _typeConvertersMap = new HashMap<Class<?>, List<Class<?>>>();
    _typeConvertersMap.put(NoteDao.class, NoteDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(FollowThroughDao.class, FollowThroughDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(GoalDao.class, GoalDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(CheckInDao.class, CheckInDao_Impl.getRequiredConverters());
    _typeConvertersMap.put(QuestionLabelDao.class, QuestionLabelDao_Impl.getRequiredConverters());
    return _typeConvertersMap;
  }

  @Override
  @NonNull
  public Set<Class<? extends AutoMigrationSpec>> getRequiredAutoMigrationSpecs() {
    final HashSet<Class<? extends AutoMigrationSpec>> _autoMigrationSpecsSet = new HashSet<Class<? extends AutoMigrationSpec>>();
    return _autoMigrationSpecsSet;
  }

  @Override
  @NonNull
  public List<Migration> getAutoMigrations(
      @NonNull final Map<Class<? extends AutoMigrationSpec>, AutoMigrationSpec> autoMigrationSpecs) {
    final List<Migration> _autoMigrations = new ArrayList<Migration>();
    return _autoMigrations;
  }

  @Override
  public NoteDao noteDao() {
    if (_noteDao != null) {
      return _noteDao;
    } else {
      synchronized(this) {
        if(_noteDao == null) {
          _noteDao = new NoteDao_Impl(this);
        }
        return _noteDao;
      }
    }
  }

  @Override
  public FollowThroughDao followThroughDao() {
    if (_followThroughDao != null) {
      return _followThroughDao;
    } else {
      synchronized(this) {
        if(_followThroughDao == null) {
          _followThroughDao = new FollowThroughDao_Impl(this);
        }
        return _followThroughDao;
      }
    }
  }

  @Override
  public GoalDao goalDao() {
    if (_goalDao != null) {
      return _goalDao;
    } else {
      synchronized(this) {
        if(_goalDao == null) {
          _goalDao = new GoalDao_Impl(this);
        }
        return _goalDao;
      }
    }
  }

  @Override
  public CheckInDao checkInDao() {
    if (_checkInDao != null) {
      return _checkInDao;
    } else {
      synchronized(this) {
        if(_checkInDao == null) {
          _checkInDao = new CheckInDao_Impl(this);
        }
        return _checkInDao;
      }
    }
  }

  @Override
  public QuestionLabelDao questionLabelDao() {
    if (_questionLabelDao != null) {
      return _questionLabelDao;
    } else {
      synchronized(this) {
        if(_questionLabelDao == null) {
          _questionLabelDao = new QuestionLabelDao_Impl(this);
        }
        return _questionLabelDao;
      }
    }
  }
}
