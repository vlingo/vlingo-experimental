# vlingo-symbio-foundationdb

The vlingo/PLATFORM implementation of vlingo/symbio for FoundationDB (currently experimental).

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
