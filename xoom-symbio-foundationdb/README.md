# vlingo-symbio-foundationdb

The VLINGO/PLATFORM implementation of VLINGO/SYMBIO `Journal` for FoundationDB (currently experimental).

Docs: https://docs.vlingo.io/vlingo-symbio/sourcing-journal-storage

## Installing FoundationDB

The following is for test purposes the most reliable way to install the database.
 
 *See [chr1st0ph/foundationdb](https://hub.docker.com/r/chr1st0ph/foundationdb/)*
 
### Downloading with Docker
```
 $ docker run -it chr1st0ph/foundationdb
```

### Starting the Server
```
 $ sudo launchctl load /Library/LaunchDaemons/com.foundationdb.fdbmonitor.plist
```

### Stopping the Server
```
 $ sudo launchctl unload /Library/LaunchDaemons/com.foundationdb.fdbmonitor.plist
```
