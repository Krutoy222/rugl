
package com.ryanm.droid.rugl.util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.Thread.UncaughtExceptionHandler;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Build;
import android.util.Log;

import com.ryanm.droid.rugl.GameActivity;

/**
 * A handy utility that catches otherwise-uncaught exceptions and
 * saves it to private storage. On next launch, the user is prompted
 * to email the data to some support address. This allows paranoid
 * users to preview the information you are gathering, helpful users
 * to add additional information, and means your application does not
 * require network access
 * 
 * @author ryanm
 */
public class ExceptionHandler implements UncaughtExceptionHandler
{
	private static final String exceptionsDir = "exceptions";

	private final static Map<String, String> extraInfo = new TreeMap<String, String>();

	/**
	 * Add some extra info that will be appended to exception logs
	 * 
	 * @param key
	 *           The key for this log info - allows you to overwrite it
	 *           at a later point
	 * @param logInfo
	 *           The log info to include in exception reports
	 */
	public static void addLogInfo( String key, String logInfo )
	{
		extraInfo.put( key, logInfo );
	}

	/**
	 * Call this at startup to register the exception handler and to
	 * check for saved exception files. Note that this is done for you
	 * in {@link GameActivity#start(com.ryanm.droid.rugl.Game, String)}
	 * if you supply a non-null support address
	 * 
	 * @param context
	 * @param address
	 *           A list of address that the report should be sent to
	 */
	public static void register( Context context, String... address )
	{
		ExceptionHandler eh = new ExceptionHandler( context );

		Thread.setDefaultUncaughtExceptionHandler( eh );

		// look for saved exceptions
		File ed = context.getDir( exceptionsDir, Context.MODE_PRIVATE );

		if( ed.listFiles().length > 0 )
		{
			Intent i = new Intent( Intent.ACTION_SEND );
			i.setType( "message/rfc822" );

			StringBuilder subject = new StringBuilder();
			StringBuilder body = new StringBuilder();

			for( File f : ed.listFiles() )
			{
				subject.append( f.getName() ).append( ", " );

				BufferedReader br;
				try
				{
					br = new BufferedReader( new FileReader( f ) );
					String line;
					while( ( line = br.readLine() ) != null )
					{
						body.append( line ).append( "\n" );
					}
				}
				catch( IOException e )
				{
					e.printStackTrace();
				}

				body.append( "\n\t----\n" );

				f.delete();
			}

			i.putExtra( Intent.EXTRA_EMAIL, address );
			i.putExtra( Intent.EXTRA_SUBJECT, subject.toString() );

			i.putExtra( Intent.EXTRA_TEXT, body.toString() );
			context.startActivity( Intent.createChooser( i, "Send error report with:" ) );
		}
	}

	private final Context context;

	private UncaughtExceptionHandler defaultHandler = Thread
			.getDefaultUncaughtExceptionHandler();

	private ExceptionHandler( Context context )
	{
		this.context = context;
	}

	@Override
	public void uncaughtException( Thread thread, Throwable ex )
	{
		File ed = context.getDir( exceptionsDir, Context.MODE_PRIVATE );
		File out =
				new File( ed, ex.getClass().getSimpleName() + "@"
						+ new Date().toString().replace( ' ', '_' ) );

		// save it
		final StringWriter result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter( result );
		ex.printStackTrace( printWriter );

		try
		{
			BufferedWriter bos = new BufferedWriter( new FileWriter( out ) );

			try
			{
				PackageInfo pi =
						context.getPackageManager()
								.getPackageInfo( context.getPackageName(), 0 );
				bos.write( "Version number = " + pi.versionCode + "\n" );
				bos.write( "Version name = " + pi.versionName + "\n" );
			}
			catch( NameNotFoundException e )
			{
				bos.write( "could not find package info" );
			}

			bos.write( new Date().toString() + "\n\n" );
			bos.write( "SDK version = " + Build.VERSION.SDK + "\n" );
			bos.write( "Manufacturer = " + Build.MANUFACTURER + "\n" );
			bos.write( "Product = " + Build.PRODUCT + "\n" );
			bos.write( "Model = " + Build.MODEL + "\n" );
			bos.write( "Device = " + Build.DEVICE + "\n" );
			bos.write( "Brand = " + Build.BRAND + "\n" );
			bos.write( "Fingerprint = " + Build.FINGERPRINT + "\n\n" );

			bos.write( result.toString() );

			bos.write( "\nOn thread " + thread.toString() );

			bos.write( "\n\nExtra log information:\n" );
			for( Map.Entry<String, String> e : extraInfo.entrySet() )
			{
				bos.write( e.getKey() );
				bos.write( ":\t" );
				bos.write( e.getValue() );
			}

			bos.close();
		}
		catch( Exception e )
		{
			Log.e( "ExceptionHandler", "Oh dear", e );
		}

		if( defaultHandler != null )
		{
			defaultHandler.uncaughtException( thread, ex );
		}
	}
}
