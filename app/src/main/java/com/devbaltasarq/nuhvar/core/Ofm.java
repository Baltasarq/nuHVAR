// nuhVAR (c) 2023/24 Balasar MIT License <baltasarq@gmail.com>


package com.devbaltasarq.nuhvar.core;


import android.content.Context;
import android.os.Environment;
import android.util.JsonReader;
import android.util.Log;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;


/** Relates the database of JSON files to objects. */
public final class Ofm {
    private static final String LogTag = Ofm.class.getSimpleName();
    public static final String FILE_PREFIX = "res" + Id.FIELD + "_";
    public static final String FILE_EXT = ".res";
    private static final String DIR_DB = "db";

    /** Prepares the ORM to operate. */
    private Ofm(Context context)
    {
        this.context = context;
        this.reset();
    }

    /** Forces a reset of the ORM, so the contents of the store is reflected
      *  in the internal data structures.
      */
    public final void reset()
    {
        Log.i( LogTag, "Preparing store..." );
        this.createDirectories();
        Log.i( LogTag, "Store ready at: " + this.dirDb.getAbsolutePath() );
    }

    /** Creates the needed directories, if they do not exist. */
    private void createDirectories()
    {
        this.dirDb = this.context.getDir( DIR_DB,  Context.MODE_PRIVATE );
        this.dirTmp = this.context.getCacheDir();
    }

    /** @return all results. */
    public List<File> getAllResults() throws IOException
    {
        final File[] ALL_RES = this.dirDb.listFiles(
                                    (dir, name) -> name.endsWith( FILE_EXT ));
        final ArrayList<File> TORET = new ArrayList<>( 0 );

        if ( ALL_RES != null ) {
            TORET.ensureCapacity( ALL_RES.length );
            TORET.addAll( Arrays.asList( ALL_RES ) );
        } else {
            throw new IOException( "i/o error" );
        }

        return TORET;
    }

    /** Removes object 'p' from the database.
      * @param ID The id of the result to remove.
      */
    public void removeResult(final Id ID) throws IOException
    {
        // Remove main object
        final File REMOVE_FILE = new File( this.dirDb, buildResultFileNameFor( ID ) );

        if ( !REMOVE_FILE.delete() ) {
            final String ERR_MSG = "Error removing file: " + REMOVE_FILE;
            Log.e( LogTag, ERR_MSG );
            throw new IOException( ERR_MSG );
        }

        Log.d( LogTag, "Result: " + ID + " deletion finished" );
    }

    /** @return a newly created temp file. */
    public File createTempFile(String prefix, String suffix) throws IOException
    {
        return File.createTempFile( prefix, suffix, this.dirTmp );
    }

    /** Stores any data object.
     * @param r The result object to store.
     */
    public void store(Result r) throws IOException
    {
        this.store( this.dirDb, r );
    }

    private void store(File dir, final Result RES) throws IOException
    {
        // Store the data
        final File TEMP_FILE = this.createTempFile(
                                    "res", RES.getId().toString() );
        final File DATA_FILE = new File( dir, buildResultFileNameFor( RES.getId() ) );
        Writer writer = null;

        try {
            Log.i( LogTag, "Storing: " + RES.toString() + " to: " + DATA_FILE.getAbsolutePath() );
            writer = openWriterFor( TEMP_FILE );
            RES.toJSON( writer );
            close( writer );

            if ( !TEMP_FILE.renameTo( DATA_FILE ) ) {
                Log.d( LogTag, "Unable to move: " + DATA_FILE );
                Log.d( LogTag, "Trying to copy: " + TEMP_FILE + " to: " + DATA_FILE );
                copy( TEMP_FILE, DATA_FILE );
            }

            Log.i( LogTag, "Finished storing." );
        } catch(IOException exc) {
            final String msg = "I/O error writing: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e( LogTag, msg );
            throw new IOException( msg );
        } catch(JSONException exc) {
            final String msg = "error creating JSON for: "
                            + DATA_FILE.toString() + ": " + exc.getMessage();
            Log.e( LogTag, msg );
            throw new IOException( msg );
        } finally {
          close( writer );
          if ( !TEMP_FILE.delete() ) {
              Log.e( LogTag, "Error removing file: " + TEMP_FILE );
          }
        }
    }

    /** Exports a given result set.
     * @param dir the directory to export the result set to.
     *             If null, then Downloads is chosen.
     * @param RES the result to export.
     * @throws IOException if something goes wrong, like a write fail.
     */
    public void exportResult(File dir, final Result RES) throws IOException
    {
        final String RES_FILE_NAME = buildResultFileNameFor( RES.getId() );

        if ( dir == null ) {
            dir = DIR_DOWNLOADS;
        }

        try {
            final String BASE_FILE_NAME = removeFileExt( RES_FILE_NAME );
            final String RRS_FILE_NAME = "rrs-" + BASE_FILE_NAME + ".rr.txt";

            // Org
            final File ORG_FILE = new File( this.dirDb, RES_FILE_NAME );
            this.store( RES );

            // Dest
            final File OUTPUT_FILE = new File( dir, RES_FILE_NAME );
            final File RR_OUTPUT_FILE = new File( dir, RRS_FILE_NAME );
            final Writer RRS_STREAM = openWriterFor( RR_OUTPUT_FILE );

            dir.mkdirs();
            copy( ORG_FILE, OUTPUT_FILE );
            RES.exportToStdTextFormat( RRS_STREAM );

            close( RRS_STREAM );
        } catch(IOException exc) {
            throw new IOException(
                    "exporting: '"
                            + RES_FILE_NAME
                            + "' to '" + dir
                            + "': " + exc.getMessage() );
        }

        return;
    }

    /** Loads a result from file.
      * @param ID the id of the result to load.
      * @return the result object loaded from file for the given id.
      * @throws IOException provided a read error happens.
      */
    public Result retrieveResult(final Id ID) throws IOException
    {
        final String FILE_NAME = buildResultFileNameFor( ID );

        return retrieveResult( new File( this.dirDb, FILE_NAME ) );
    }

    /** Builds the file name for the given result.
      * @param ID the given id for the result file.
      * @return the built file name.
      */
    public static String buildResultFileNameFor(final Id ID)
    {
        return FILE_PREFIX + ID.get() + FILE_EXT;
    }

    /** Loads the given rec of a result file.
      * @param f the file where the result is stored.
      * @return the record, as a string.
      * @throws IOException if parsing or loading goes wrong.
      */
    public static Map.Entry<Id, String> readIdAndRecFromResultFile(File f)
            throws IOException
    {
        final JsonReader JSON_READER = new JsonReader( openReaderFor( f ) );
        String rec = "";
        Id id = null;

        // Load data
        try {
            JSON_READER.beginObject();
            while ( JSON_READER.hasNext() ) {
                final String nextName = JSON_READER.nextName();

                if ( nextName.equals( Id.FIELD ) ) {
                    id = Persistent.readIdFromJSON( JSON_READER );
                }
                else
                if ( nextName.equals( Result.FIELD_REC ) ) {
                    rec = JSON_READER.nextString();
                } else {
                    JSON_READER.skipValue();
                }

                if ( id != null
                  && !rec.isEmpty() )
                {
                    break;
                }
            }
        } catch (JSONException exc)
        {
            throw new IOException( exc );
        }

        if ( id == null
          || rec.isBlank() )
        {
            throw new IOException( "Ofm.readIdAndRec(): missing info reading id and rec" );
        }

        return Map.entry( id, rec );
    }

    /** @return the result object loaded from file f. */
    private static Result retrieveResult(File f) throws IOException
    {
        Result toret;
        Reader reader = null;

        try {
            reader = openReaderFor( f );
            toret = Result.fromJSON( reader );
        } catch(IOException|JSONException exc)
        {
            final String msg = "retrieveResult(f) reading JSON: " + exc.getMessage();
            Log.e( LogTag, msg );

            throw new IOException( msg );
        } finally {
            close( reader );
        }

        return toret;
    }

    public static Writer openWriterFor(File f) throws IOException
    {
        BufferedWriter toret;

        try {
            final OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    new FileOutputStream( f ),
                    StandardCharsets.UTF_8.newEncoder() );

            toret = new BufferedWriter( outputStreamWriter );
        } catch (IOException exc) {
            Log.e( LogTag,"Error creating writer for file: " + f );
            throw exc;
        }

        return toret;
    }

    public static BufferedReader openReaderFor(File f) throws IOException
    {
        BufferedReader toret;

        try {
            toret = openReaderFor( new FileInputStream( f ) );
        } catch (IOException exc) {
            Log.e( LogTag,"Error creating reader for file: " + f.getName() );
            throw exc;
        }

        return toret;
    }

    private static BufferedReader openReaderFor(InputStream inStream)
    {
        final InputStreamReader inputStreamReader = new InputStreamReader(
                inStream,
                StandardCharsets.UTF_8.newDecoder() );

        return new BufferedReader( inputStreamReader );
    }

    /** Closes a writer stream. */
    public static void close(Writer writer)
    {
        try {
            if ( writer != null ) {
                writer.close();
            }
        } catch(IOException exc)
        {
            Log.e( LogTag, "closing writer: " + exc.getMessage() );
        }
    }

    /** Closes a reader stream. */
    public static void close(Reader reader)
    {
        try {
            if ( reader != null ) {
                reader.close();
            }
        } catch(IOException exc)
        {
            Log.e( LogTag, "closing reader: " + exc.getMessage() );
        }
    }

    /** Closes a JSONReader stream. */
    private static void close(JsonReader jsonReader)
    {
        try {
            if ( jsonReader != null ) {
                jsonReader.close();
            }
        } catch(IOException exc)
        {
            Log.e( LogTag, "closing json reader: " + exc.getMessage() );
        }
    }

    /** Copies a given file to a destination, overwriting if necessary.
      * @param source The File object of the source file.
      * @param dest The File object of the destination file.
      * @throws IOException if something goes wrong while copying.
      */
    private static void copy(File source, File dest) throws IOException
    {
        final String errorMsg = "error copying: " + source + " to: " + dest + ": ";
        InputStream is;
        OutputStream os;

        try {
            is = new FileInputStream( source );
            os = new FileOutputStream( dest );

            copy( is, os );
        } catch(IOException exc)
        {
            Log.e( LogTag, errorMsg + exc.getMessage() );
            throw new IOException( errorMsg );
        }

        return;
    }

    /** Copies data from a given stream to a destination File, overwriting if necessary.
     * @param is The input stream object to copy from.
     * @param dest The File object of the destination file.
     * @throws IOException if something goes wrong while copying.
     */
    private static void copy(InputStream is, File dest) throws IOException
    {
        final String errorMsg = "error copying input stream -> " + dest + ": ";
        OutputStream os;

        try {
            os = new FileOutputStream( dest );

            copy( is, os );
        } catch(IOException exc)
        {
            Log.e( LogTag, errorMsg + exc.getMessage() );
            throw new IOException( errorMsg );
        }

        return;
    }

    /** Copies from a stream to another one.
     * @param is The input stream object to copy from.
     * @param os The output stream object of the destination.
     * @throws IOException if something goes wrong while copying.
     */
    private static void copy(InputStream is, OutputStream os) throws IOException
    {
        final byte[] buffer = new byte[1024];
        int length;

        try {
            while ( ( length = is.read( buffer ) ) > 0 ) {
                os.write( buffer, 0, length );
            }
        } finally {
            try {
                if ( is != null ) {
                    is.close();
                }

                if ( os != null ) {
                    os.close();
                }
            } catch(IOException exc) {
                Log.e( LogTag, "Copying file: error closing streams: " + exc.getMessage() );
            }
        }

        return;
    }

    /** @return the file extension, extracted from param.
     * @param file The file, as a File.
     */
    public static String extractFileExt(File file)
    {
        return extractFileExt( file.getPath() );
    }

    /** @return the file extension, extracted from param.
      * @param fileName The file name, as a String.
      */
    public static String extractFileExt(String fileName)
    {
        String toret = "";

        if ( fileName != null
          && !fileName.trim().isEmpty() )
        {
            final int posDot = fileName.trim().lastIndexOf( "." );


            if ( posDot >= 0
              && posDot < ( fileName.length() - 1 ) )
            {
                toret = fileName.substring( posDot + 1 );
            }
        }

        return toret;
    }

    /** @return the given file name, after extracting the extension.
      * @param fileName the file name to remove the extension from.
      * @return the file name without extension.
      */
    public static String removeFileExt(String fileName)
    {
        final int DOT_POS = fileName.lastIndexOf( '.' );
        String toret = fileName;

        if ( DOT_POS > -1 ) {
            toret = fileName.substring(0, DOT_POS);
        }

        return toret;
    }

    /** Gets the already open database.
     * @return The Orm singleton object.
     */
    public static Ofm get()
    {
        if ( instance == null ) {
            final String ERROR_MSG = "Orm database manager not created yet.";

            Log.e( LogTag, ERROR_MSG );
            throw new IllegalArgumentException( ERROR_MSG );
        }

        return instance;
    }

    /** Initialises the already open database.
     * @param context the application context this database will be working against.
     * @param fileNameAdapter A lambda or referenced method of the signature: (String x) -> x
     *                        this will convert file names to the needed standard, which is
     *                        lowercase and no spaces ('_' instead).
     * @see Function
     */
    public static void init(Context context, PlainStringEncoder fileNameAdapter)
    {
        if ( instance == null ) {
            instance = new Ofm( context );
        }

        Ofm.fileNameAdapter = fileNameAdapter;
        return;
    }

    private File dirDb;
    private File dirTmp;
    private final Context context;

    private static PlainStringEncoder fileNameAdapter;
    private static Ofm instance;
    private static File DIR_DOWNLOADS = Environment.getExternalStoragePublicDirectory(
                                                                Environment.DIRECTORY_DOWNLOADS );
}
