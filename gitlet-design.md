# Gitlet Design Document

**Name**:

## Classes and Data Structures

### Repository

#### Fields

1. ```public static final File CWD```

* Represents the current working directory

2. ```public static final File GITLET_DIR```

* Represents the .gitlet directory

### Refs

#### Fields

1. ```public static final File REF_DIR```

* Represents the refs directory

2. ```public static final File HEAD```

* Represents the HEAD file

3. ```public static final File HEADS_DIR```

* Represents the refs/heads directory

4. ```public static final File REMOTES_DIR```

* Represents the refs/remotes directory

### Commit

#### Fields

1. ```private String Message```

* Represents the commit message

2. ```private Date timestamp```

* Represents the time the commit was created

3. ``private Commit parent``

* Points to the parent commit

4. ```private String author```

* The author of this commit

5. ```private Map trackedFiles```

* A <sha1 hash, file> mapping of the files that this commit keeps track of

## Algorithms

## Persistence

* HEAD -> points to the current branch head stored in refs/heads
* STAGING_AREA -> keeps track of files added with ``add`` command. these files are 'staged' and will be commited
  with ``commit`` command
* refs/heads -> this directory keeps the head commit reference for each branch
* Objects -> directory that stores serialized commits and files that were commited
