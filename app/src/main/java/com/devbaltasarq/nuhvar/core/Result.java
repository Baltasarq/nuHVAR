// (c) nuHVAR 2023/24 Baltasar MIT License <baltasarq@gmail.com>


package com.devbaltasarq.nuhvar.core;


import androidx.annotation.NonNull;
import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;

import org.json.JSONException;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Collection;
import java.util.ArrayList;


/** Represents the results of a given experiment. */
public class Result extends Persistent {
    public static final String LOG_TAG = Result.class.getSimpleName();
    public static final String FIELD_HEART_BEAT_AT = "heart_beat_at";
    public static final String FIELD_MILLIS = "millis";
    public static final String FIELD_DATE = "date";
    public static final String FIELD_TIME = "time";
    public static final String FIELD_REC = "record";
    public static final String FIELD_EVENTS = "beat_events";


    public static final class Builder {
        public Builder(String rec, long dateTime)
        {
            this.rec = rec;
            this.dateTime = dateTime;
            this.events = new ArrayList<>( 20 );
        }

        /** @return the record name. */
        public String getRecord()
        {
            return this.rec;
        }

        /** Sets the record name.
          * @param rec the new record name.
          */
        public void setRecord(String rec)
        {
            this.rec = rec;
        }

        /** Adds a new Event to the list.
          * @param event the new Event.
          * @see BeatEvent
          */
        public void add(BeatEvent event)
        {
            this.events.add( event );
        }

        /** Adds all the given events.
          * @param events a collection of events.
          * @see BeatEvent
          */
        public void addAll(Collection<BeatEvent> events)
        {
            this.events.addAll( events );
        }

        /** Clears all the stored events. */
        public void clear()
        {
            this.events.clear();
        }

        /** @return all stored events up to this moment. */
        public BeatEvent[] getEvents()
        {
            return this.events.toArray( new BeatEvent[ 0 ] );
        }

        /** @return the appropriate Result object, given the current data.
          * @see Result
          */
        public Result build(long elapsedMillis)
        {
            return new Result(
                            Id.create(),
                            this.dateTime,
                            elapsedMillis,
                            this.rec,
                            this.events.toArray( new BeatEvent[ 0 ] ) );
        }

        private String rec;
        private final long dateTime;
        private final ArrayList<BeatEvent> events;
    }

    /** Base class for events. */
    public static final class BeatEvent {
        public BeatEvent(long millis, long heartBeatAt)
        {
            this.millis = millis;
            this.heartBeatAt = heartBeatAt;
        }

        /** The time of the heart beat. */
        public long getHeartBeatAt()
        {
            return this.heartBeatAt;
        }

        /** The time the heart beat happened. */
        public long getMillis()
        {
            return this.millis;
        }

        @Override
        public int hashCode()
        {
            return Long.valueOf( this.getMillis() ).hashCode()
                    + ( 11 * Long.valueOf( this.getHeartBeatAt() ).hashCode() );
        }

        @Override
        public boolean equals(Object obj2)
        {
            boolean toret = false;

            if ( obj2 instanceof BeatEvent EVT) {
                toret = ( this.getHeartBeatAt() == EVT.getHeartBeatAt()
                       && this.getMillis() == EVT.getMillis() );
            }

            return toret;
        }

        public void writeToJSON(JsonWriter jsonWriter) throws IOException
        {
            jsonWriter.beginObject();
            jsonWriter.name( FIELD_MILLIS ).value( this.getMillis() );
            jsonWriter.name( FIELD_HEART_BEAT_AT ).value( this.getHeartBeatAt() );
            jsonWriter.endObject();
        }

        public static BeatEvent fromJSON(JsonReader jsonReader) throws JSONException
        {
            BeatEvent toret;
            long millis = -1L;
            long heartBeat = -1L;

            // Load data
            try {
                jsonReader.beginObject();
                while ( jsonReader.hasNext() ) {
                    final String NEXT_NAME = jsonReader.nextName();

                    if ( NEXT_NAME.equals( FIELD_MILLIS ) ) {
                        millis = jsonReader.nextLong();
                    }
                    else
                    if ( NEXT_NAME.equals( FIELD_HEART_BEAT_AT ) ) {
                        heartBeat = jsonReader.nextLong();
                    } else {
                        jsonReader.skipValue();
                    }
                }

                jsonReader.endObject();
            } catch(IOException exc)
            {
                final String ERROR_MSG = "Creating event from JSON: " + exc.getMessage();

                Log.e( LOG_TAG, ERROR_MSG );
                throw new JSONException( ERROR_MSG );
            }

            // Chk
            if ( heartBeat < 0
              || millis < 0 )
            {
                final String MSG = "Creating event from JSON: invalid pos/hr time.";

                Log.e( LOG_TAG, MSG );
                throw new JSONException( MSG );
            } else {
                toret = new BeatEvent( millis, heartBeat );
            }

            return toret;
        }

        private final long millis;
        private final long heartBeatAt;
    }

    /** Creates a new Result, in which the events of the experiment will be stored.
     * @param id   the id of the result.
     * @param dateTime the moment (in millis) this experiment was collected.
     * @param durationInMillis the duration of the experiment in milliseconds.
     * @param rec the record label.
     */
    public Result(Id id, long dateTime, long durationInMillis,
                  String rec, BeatEvent[] events)
    {
        super( id );

        this.durationInMillis = durationInMillis;
        this.dateTime = dateTime;
        this.rec = rec;
        this.events = events;
    }

    /** @return the date for this results. */
    public long getTime()
    {
        return this.dateTime;
    }

    /** @return the record label */
    public String getRec()
    {
        return this.rec;
    }

    @Override
    public int hashCode()
    {
        return this.getId().hashCode()
                + ( 11 * this.getRec().hashCode() );
    }

    @Override
    public boolean equals(Object o)
    {
        boolean toret = false;

        if ( o instanceof Result ro ) {
            if ( this.getRec().equals( ro.getRec() ) ) {
                toret = true;
            }
        }

        return toret;
    }

    /** @return all events in this result. Warning: the list can be huge. */
    public BeatEvent[] buildHeartBeatEventsList()
    {
        return Arrays.copyOf( this.events, this.events.length );
    }

    /** @return the heart beats time distance, as a long[]. */
    public long[] buildHeartBeatsList()
    {
        final long[] TORET = new long[ this.events.length ];

        // Create (grr... Long != long... come on...)
        int pos = 0;
        for(BeatEvent evt: this.events) {
            TORET[ pos++ ] = evt.getHeartBeatAt();
        }

        return TORET;
    }

    /** Creates the standard pair of text files, one for heatbeats,
      * and another one to know when the activity changed.
      */
    public void exportToStdTextFormat(final Writer BEATS_STREAM)
        throws IOException
    {
        for(final BeatEvent BEAT_IT: this.events) {
            BEATS_STREAM.write( Long.toString( BEAT_IT.getHeartBeatAt() ) );
            BEATS_STREAM.write( '\n' );
        }

        return;
    }

    /** @return the number of events stored. */
    public int size()
    {
        return this.events.length;
    }

    @Override
    public void writeToJSON(JsonWriter jsonWriter) throws IOException
    {
        this.writeIdToJSON( jsonWriter );

        jsonWriter.name( FIELD_DATE ).value( this.getTime() );
        jsonWriter.name( FIELD_TIME ).value( this.getDurationInMillis() );
        jsonWriter.name( FIELD_REC ).value( this.getRec() );

        jsonWriter.name( FIELD_EVENTS ).beginArray();
        for(final BeatEvent BEAT_IT: this.events) {
            BEAT_IT.writeToJSON( jsonWriter );
        }

        jsonWriter.endArray();
    }

    /** @return the duration in millis. Will throw if the experiment is not finished yet. */
    public long getDurationInMillis()
    {
        return this.durationInMillis;
    }

    @Override @NonNull
    public String toString()
    {
        return this.getId() + "@" + this.getTime() + ": " + this.getRec();
    }

    public static Result fromJSON(Reader reader) throws JSONException
    {
        final JsonReader JSON_READER = new JsonReader( reader );
        final ArrayList<BeatEvent> EVENTS = new ArrayList<>( 20 );
        long dateTime = -1L;
        long durationInMillis = -1L;
        String rec = "";
        Result toret = null;
        Id id = null;

        // Load data
        try {
            JSON_READER.beginObject();
            while ( JSON_READER.hasNext() ) {
                final String nextName = JSON_READER.nextName();

                if ( nextName.equals( FIELD_DATE ) ) {
                    dateTime = JSON_READER.nextLong();
                }
                else
                if ( nextName.equals( FIELD_TIME ) ) {
                    durationInMillis = JSON_READER.nextLong();
                }
                else
                if ( nextName.equals( Id.FIELD ) ) {
                    id = readIdFromJSON( JSON_READER );
                }
                else
                if ( nextName.equals( FIELD_REC ) ) {
                    rec = JSON_READER.nextString();
                }
                else
                if ( nextName.equals( FIELD_EVENTS ) ) {
                    JSON_READER.beginArray();
                    while( JSON_READER.hasNext() ) {
                        EVENTS.add( BeatEvent.fromJSON( JSON_READER ) );
                    }

                    JSON_READER.endArray();
                } else {
                    JSON_READER.skipValue();
                }
            }
        } catch(IOException exc)
        {
            final String ERROR_MSG = "Creating result from JSON: " + exc.getMessage();

            Log.e(LOG_TAG, ERROR_MSG );
            throw new JSONException( ERROR_MSG );
        }

        // Chk
        if ( id == null
          || dateTime < 0
          || durationInMillis < 0 )
        {
            final String MSG_ERROR = "Creating result from JSON: invalid or missing data.";

            Log.e( LOG_TAG, MSG_ERROR );
            throw new JSONException( MSG_ERROR );
        } else {
            if ( rec.isEmpty() ) {
                rec = "r";
            }

            toret = new Result( id,
                                dateTime,
                                durationInMillis,
                                rec,
                                EVENTS.toArray( new BeatEvent[ 0 ] ) );
        }

        return toret;
    }

    private final long durationInMillis;
    private final String rec;
    private final long dateTime;
    private final BeatEvent[] events;
}
