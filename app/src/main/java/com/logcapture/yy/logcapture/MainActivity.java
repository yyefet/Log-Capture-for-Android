/**
 *  LogCapture for Android
 *  Author: Yehuda Yefet
 *  Open Source
 */

package com.logcapture.yy.logcapture;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.widget.Toast;


public class MainActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        //Clear the logs when button clicked.
        Button clearLogs = (Button) findViewById(R.id.clearLogs);
        clearLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                //clear logs and app generated log file here
                try {
                    //delete androidlog.txt file
                    String filename = "androidlog.txt";
                    File sdCard = Environment.getExternalStorageDirectory();
                    File file = new File (sdCard.getAbsolutePath(),filename);
                    file.delete();

                    if(!file.exists()) {
                        Log.v("LOGCAPTURE", "file androidlog.txt deleted from sdcard");
                    }

                    Process process = Runtime.getRuntime().exec("logcat -c");
                    BufferedReader bufferedReader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));

                    //Logs cleared
                    Toast.makeText(getBaseContext(), "Logs cleared",
                            Toast.LENGTH_SHORT).show();
                    Log.v("LOGCAPTURE", "logs cleared");

                } catch (Exception e) {
                    e.printStackTrace();
                    Log.v("LOGCAPTURE", "could not delete file androidlog.txt");
                }

            }//onclick
        });//clicklistener

        //write logs to file and textview when button clicked.
        Button logCapture = (Button) findViewById(R.id.logCaptureBtn);
        logCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //run the thread
                Thread thread = new Thread(new LogRunnable());
                thread.run(); //in current thread
                Log.v("LOGCAPTURE", "LogRunnable thread started");
            }//onclick
        });//clicklistener


        //launch email intent when button clicked.
        Button emailLogs = (Button) findViewById(R.id.emailLogBtn);
        emailLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //run the thread
                Thread threademail = new Thread(new EmailRunnable());
                threademail.run(); //in current thread
                Log.v("LOGCAPTURE", "EmailRunnable thread started");
            }//onclick
        });//clicklistener

    }//oncreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }




    private class LogRunnable implements Runnable {

        @Override
        public void run() {

                runOnUiThread(new Runnable() {
                //Runnable runnable = new Runnable() {

                    @Override
                    public void run() {

                        try {
                            Process process = Runtime.getRuntime().exec("logcat -d -v long"); //was -d
                            BufferedReader bufferedReader = new BufferedReader(
                                    new InputStreamReader(process.getInputStream()));

                            Log.v("LOGCAPTURE", "logcat command executed");

                            StringBuilder log=new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                log.append(line);
                            }

                            //add newlines
                            String logCapture = Pattern.compile("\\[ ").matcher(log.toString()).replaceAll("\n[ ");

                            //write logs to TextView
                            TextView tv = (TextView)findViewById(R.id.logText);
                            tv.setMovementMethod(new ScrollingMovementMethod());
                            tv.setText(logCapture);

                            try {

                                String filename = "androidlog.txt";
                                File sdCard = Environment.getExternalStorageDirectory();
                                File file = new File (sdCard.getAbsolutePath(),filename);
                                file.createNewFile();

                                    byte[] data = logCapture.getBytes();
                                    try {
                                        if(file.exists()) {
                                            Log.v("LOGCAPTURE", "androidlog.txt file exists: true");

                                            OutputStream fo = new FileOutputStream(file);
                                            //write the data
                                            fo.write(data);
                                            //close to avoid memory leaks
                                            fo.close();

                                            Log.v("LOGCAPTURE", "androidlog.txt file write: true");
                                        }

                                        //display file saved message
                                        Toast.makeText(getBaseContext(), "File saved successfully!" + file,
                                                Toast.LENGTH_SHORT).show();

                                    } catch (FileNotFoundException e) {
                                        e.printStackTrace();
                                    }



                                } catch (Exception e) {
                                    e.printStackTrace();
                                }


                        } catch (IOException e) {
                        }//exception

                    }

                });//runnableonUI
                //};//runnable
        }

    }//runnable




    private class EmailRunnable implements Runnable {

        @Override
        public void run() {

            runOnUiThread(new Runnable() {
                //Runnable runnable = new Runnable() {

                @Override
                public void run() {

                    try {
                            String bodystr = fetchLogData();//moved to its own function.
                            //email intent
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("message/rfc822");
                            i.putExtra(Intent.EXTRA_EMAIL, new String[]{""});//email to address
                            i.putExtra(Intent.EXTRA_SUBJECT, "Log Capture");
                            i.putExtra(Intent.EXTRA_TEXT   , bodystr);
                            i.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"
                                    + Environment.getExternalStorageDirectory().getAbsolutePath()
                                    + File.separator + "androidlog.txt"));

                            try {
                                startActivity(Intent.createChooser(i, "Sending mail..."));
                                Log.v("LOGCAPTURE", "Email intent started");
                            } catch (android.content.ActivityNotFoundException ex) {
                                Toast.makeText(getBaseContext(), "An Error Happened ", Toast.LENGTH_SHORT).show();
                                Log.v("LOGCAPTURE", "Email intent failure");
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                }

            });//runnableonUI
            //};//runnable
        }//run

    }//runnable


    //replace all
    public static void replaceAll(StringBuilder sb, Pattern pattern, String replacement) {
        Matcher m = pattern.matcher(sb);
        while(m.find()) {
            sb.replace(m.start(), m.end(), replacement);
        }
    }



    //test http connection
    public boolean testConnect(String url) {
        try{
            URL myUrl = new URL(url);
            URLConnection connection = myUrl.openConnection();
            connection.setConnectTimeout(4000);
            connection.connect();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String fetchLogData(){
        //fetch log data
        try {
        //Android device info
        String androidVersion = "Android Version: " + Build.VERSION.RELEASE;
        String androidBrand = "Android Brand: " + Build.BRAND;
        String androidManufacturer = "Android Manufacturer: " + Build.MANUFACTURER;
        String androidProduct = "Android Product: " + Build.PRODUCT;

        //network info
        ConnectivityManager cm = (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();

        //ip info
        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        DhcpInfo dhcpinfo = wifiManager.getDhcpInfo();
        String wifiName = wifiInfo.getSSID();

        //proxy info
        String proxyAddress = System.getProperty("http.proxyHost");
        String portStr = System.getProperty("http.proxyPort");

        //netstat
        Process su = Runtime.getRuntime().exec("netstat -lptu ");
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(su.getInputStream()));

        StringBuilder netstat=new StringBuilder();
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            netstat.append(line);
        }

        //add newlines
        String netstatFormatted = Pattern.compile("tcp").matcher(netstat.toString()).replaceAll("\ntcp");
        netstatFormatted = Pattern.compile("udp").matcher(netstatFormatted).replaceAll("\nudp");

        //construct email body
        String bodystr =    "Log file path: "
                + "\nfile://" + Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "androidlog.txt"
                + "\n\n" + androidVersion
                + "\n" + androidBrand
                + "\n" + androidManufacturer
                + "\n" + androidProduct
                + "\n\nNetwork Info: "
                + "\n" + activeNetwork
                + "\nSSID: "
                + "\n" + wifiName
                + "\n\nDHCP Info: "
                + "\n" + dhcpinfo
                + "\n\nProxy Info: "
                + "\nHost: " + proxyAddress
                + "\nPort: " + portStr
                + "\n\nConnection Test: "
                + "\nwww.google.com: "          + testConnect("google.com")
                + "\naccounts.google.com: "     + testConnect("accounts.google.com")
                + "\naccounts.gstatic.com: "    + testConnect("accounts.gstatic.com")
                + "\napi.google.com: "          + testConnect("api.google.com")
                + "\nntp.org: "                 + testConnect("ntp.org")
                + "\nglpals.com: "              + testConnect("glpals.com")
                + "\nakamai.net: "              + testConnect("akamai.net")
                + "\ngoogleapis.com: "          + testConnect("googleapis.com")
                + "\nggpht.com: "               + testConnect("ggpht.com")
                + "\ngoogleusercontent.com: "   + testConnect("googleusercontent.com")
                + "\ngoogle-analytics.com: "    + testConnect("google-analytics.com")
                + "\ngstatic.com: "             + testConnect("gstatic.com")
                + "\nandroid.com: "             + testConnect("android.com")
                + "\ngvt1.com: "                + testConnect("gvt1.com")
                + "\n\nNetstat:"
                + "\n" + netstatFormatted;

            //return bodystr on success
            return bodystr;

        } catch (Exception e) {
            e.printStackTrace();
            //return nothing on failure
            return "";
        }


    }//fetchLogData

}//main activity



