package com.devbaltasarq.nuhvar.core;


import android.util.JsonReader;
import android.util.JsonWriter;
import android.util.Log;
import org.json.JSONException;

import java.io.IOException;
import java.io.Writer;


/** Represents classes that can be stored and retrieved using JSON. */
public abstract class Persistent {
    private static final String LOG_TAG = Persistent.class.getSimpleName();
    /** Creates a new object that can be stored in the db. */
    public Persistent(Id id)
    {
        this.id = id.copy();
    }

    /** Assigns new ids to this object. Useful when storing the object for the first time. */
    public void updateIds()
    {
        this.id = Id.create();
    }

    /** @return The id of this object. */
    public Id getId()
    {
        return this.id;
    }

    /** Writes the common properties of a persistent object to JSON.
     * @param wrt The writer to write to.
     * @throws JSONException throws it when there are problems with the stream.
     */
    public void toJSON(Writer wrt) throws JSONException
    {
        final String ErrorMessage = "Writing persistent object to JSON: ";
        final JsonWriter jsonWriter = new JsonWriter( wrt );

        try {
            jsonWriter.beginObject();
            this.writeToJSON( jsonWriter );
            jsonWriter.endObject();
        } catch(IOException exc)
        {
            Log.e( LOG_TAG, ErrorMessage + exc.getMessage() );
            throw new JSONException( exc.getMessage() );
        } finally {
            try {
                jsonWriter.close();
            } catch(IOException exc) {
                Log.e(LOG_TAG, ErrorMessage + exc.getMessage() );
            }
        }

        return;
    }

    /** Writes the common properties of an Activity to JSON.
     * @param jsonWriter The JSON writer to write to.
     * @throws IOException throws it when there are problems with the stream.
     */
    public abstract void writeToJSON(JsonWriter jsonWriter) throws IOException;

    /** Writes the identification of the object to a json writer. */
    protected void writeIdToJSON(JsonWriter jsonWriter) throws IOException
    {
        jsonWriter.name( Id.FIELD ).value( this.getId().get() );
    }

    public static Id readIdFromJSON(JsonReader jsonReader) throws IOException, JSONException
    {
        Id toret = null;

        try {
            toret = Id.fromIdAsLong( jsonReader.nextLong() );
        } catch(IOException exc) {
            throw new JSONException( "read id: no valid data" );
        }

        return toret;
    }

    private Id id;
}
