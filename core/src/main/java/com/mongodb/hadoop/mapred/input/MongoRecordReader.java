// MongoRecordReader.java
/*
 * Copyright 2010 10gen Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mongodb.hadoop.mapred.input;

import com.mongodb.*;
import com.mongodb.hadoop.io.*;
import com.mongodb.hadoop.input.MongoInputSplit;
import org.apache.commons.logging.*;
import org.apache.hadoop.mapred.*;
import org.bson.*;


@SuppressWarnings( "deprecation" )
public class MongoRecordReader implements RecordReader<BasicDBObject, BasicDBObject> {

    public MongoRecordReader( MongoInputSplit split ){
        _cursor = split.getCursor();
        _keyField = split.getKeyField();
    }

    public void close(){
        if ( _cursor != null )
            _cursor.close();
    }

    public BasicDBObject createKey(){
        return new BasicDBObject();
    }


    public BasicDBObject createValue(){
        return new BasicDBObject();
    }

    public BasicDBObject getCurrentKey(){
        return new BasicDBObject( "_id", _current.get(_keyField != null ? _keyField : "_id") );
    }

    public BasicDBObject getCurrentValue(){
        return _current;
    }

    public float getProgress(){
        try {
            if ( _cursor.hasNext() ){
                return 0.0f;
            }
            else{
                return 1.0f;
            }
        }
        catch ( MongoException e ) {
            return 1.0f;
        }
    }

    public long getPos(){
        return 0; // no progress to be reported, just working on it
    }

    public void initialize( InputSplit split, TaskAttemptContext context ){
        _total = 1.0f;
    }

    public boolean nextKeyValue(){
        try {
            if ( !_cursor.hasNext() )
                return false;

            _current = (BasicDBObject)_cursor.next();
            _seen++;

            return true;
        }
        catch ( MongoException e ) {
            return false;
        }
    }

    public boolean next( BasicDBObject key, BasicDBObject value ){
        if ( nextKeyValue() ){
            log.debug( "Had another k/v" );
            key.put( "_id", getCurrentKey().get( "_id" ) );
            value.clear();
            value.putAll( (BSONObject)getCurrentValue() );
            return true;
        }
        else{
            log.info( "Cursor exhausted." );
            return false;
        }
    }

    private final DBCursor _cursor;
    private BasicDBObject _current;
    private float _seen = 0;
    private float _total;
    private String _keyField;

    private static final Log log = LogFactory.getLog( MongoRecordReader.class );
}