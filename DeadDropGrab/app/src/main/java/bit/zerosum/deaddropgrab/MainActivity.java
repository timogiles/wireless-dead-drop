package bit.zerosum.deaddropgrab;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.os.Environment;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URLConnection;
import java.net.URL;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    //when this is set to true, the search for the AP stops
    Boolean DeadDropRX = false;

    //recieve wifi scan broadcast
    private BroadcastReceiver wifiScanReceiver;
    private ProgressDialog progressDialog;

    Button activateButton;
    EditText APname, OutFile, InputURLbox;
    TextView terminal;
    String SSID;
    String OutFileName;
    String InputURL;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //pull in the UI elements from activity_main.xml, give them local names
        activateButton=(Button)findViewById(R.id.activateButton);
        APname=(EditText)findViewById(R.id.APname);
        OutFile=(EditText)findViewById(R.id.OutFilename);
        InputURLbox=(EditText)findViewById(R.id.editText_URL);

        //This code runs when the "Activate" button is pressed
        activateButton.setOnClickListener(new OnClickListener(){
            public void onClick(View v){
                WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

                activateButton.setBackgroundColor(Color.GREEN);
                DeadDropRX = false;

                //if Wifi is off, turn it on
                if (wifi.isWifiEnabled() == false) {
                    wifi.setWifiEnabled(true);
                }

                //Register the "WifiScanReceiver" code to run anytime WifiManager completes a scan of available APs
                wifiScanReceiver = new WifiScanReceiver();
                registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

                //Store the AP name entered by the user as a local string
                SSID = APname.getText().toString();
                OutFileName = OutFile.getText().toString();
                InputURL = InputURLbox.getText().toString();

                //Start a scan of available APs
                wifi.startScan();
            }
        });
    }

    //this is called each time the WifiManager completes a scan of available APs
    public class WifiScanReceiver extends BroadcastReceiver {
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        @Override
        public void onReceive(Context c, Intent intent) {
            List<ScanResult> WifiScanList = wifi.getScanResults();

            //if the file has already been received, don't process the list or initiate another scan
            if (!DeadDropRX) {
                for (int i = 0; i < WifiScanList.size(); i++) {
                    Log.d(Integer.toString(i), WifiScanList.get(i).SSID);
                    if (WifiScanList.get(i).SSID.equals(SSID)) {
                        //Our target SSID was found in the scan
                        Toast.makeText(getApplicationContext(), "AP Found In Scan", Toast.LENGTH_SHORT).show();

                        //check if we are currently connect to the target SSID
                        //NOTE for some reason the SSID here has quotes in the string, so quotes have to be added for comparison
                        if (wifi.getConnectionInfo().getSSID().toString().equals("\"" + SSID + "\"")) {
                            //if we are connected to the AP, download the file
                            DownloadFile();
                        }else {
                            //the target SSID was found be we aren't currently connected to it, so connect.
                            WifiConnectSSID(SSID);
                        }
                    }
                }
                //initiate another wifi scan if the file has not yet been downloaded.
                //NOTE that this isn't necessary once the connection to the AP has been initiated,
                //but it provides convenient timing so the scans will continue until the file is successfully
                //downloaded
                wifi.startScan();
            }else{
                //file download is complete, change the color of the button
                activateButton.setBackgroundColor(Color.GRAY);
            }
        }
    }

    //Connect to a specific wifi SSID
    public void WifiConnectSSID(String SSID){
        WifiManager wifi = (WifiManager) getSystemService(Context.WIFI_SERVICE);

        List<WifiConfiguration> list = wifi.getConfiguredNetworks();
        for( WifiConfiguration i : list ) {
            if(i.SSID != null && i.SSID.equals("\"" + SSID + "\"")) {
                wifi.disconnect();
                wifi.enableNetwork(i.networkId, true);
                wifi.reconnect();
                break;
            }
        }
    }

    //download the file of interest from the AP
    private void DownloadFile(){
        //progressDialog = ProgressDialog.show(this, "", "Downloading file");
        final String url = InputURL;

        //downloads have to run from a separate thread
        new Thread() {
            public void run() {
                int count;
                InputStream in = null;
                Message msg = Message.obtain();
                msg.what = 1;

                try {
                    //connect the file to an input stream
                    in = openHttpConnection(url);
                    if (in != null) {
                        //set the directory to save the downloaded file to.
                        File path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsoluteFile();
                        //open the file for output
                        File file = new File(path, OutFileName);
                        //delete the file if it already exists
                        if (file.exists()){
                            file.delete();
                            Log.d("1", "file deleted!!!!!!");
                        }
                        //connect an output stream to the file
                        FileOutputStream out = new FileOutputStream(file);
                        //iterate through the input file to store it all in the output file
                        byte data[] = new byte[1024];
                        long total = 0;
                        while ((count = in.read(data)) != -1) {
                            total += count;
                            // writing data to file
                            out.write(data, 0, count);
                        }
                        // flush output stream
                        out.flush();
                        // close output stream
                        out.close();
                        //close input stream
                        in.close();
                        //setting this variable to true indicates that the file has been received and we now longer need to search for it.
                        DeadDropRX = true;

                    }else{
                        Log.d("1","Inputstream was null");
                    }
                }catch (IOException e1) {
                    e1.printStackTrace();
                }
                //messageHandler.sendMessage(msg);
            }
        }.start();
    }

    //connect the web page URL to an input stream for reading
    private InputStream openHttpConnection(String urlStr) {
        InputStream in = null;
        int resCode = -1;

        try {
            URL url = new URL(urlStr);
            URLConnection urlConn = url.openConnection();

            if (!(urlConn instanceof HttpURLConnection)) {
                Log.d("28", "URL is not an http URL");
                throw new IOException("URL is not an Http URL");
            }

            HttpURLConnection httpConn = (HttpURLConnection) urlConn;
            httpConn.setAllowUserInteraction(false);
            httpConn.setInstanceFollowRedirects(true);
            httpConn.setRequestMethod("GET");
            httpConn.connect();
            resCode = httpConn.getResponseCode();

            if (resCode == HttpURLConnection.HTTP_OK) {
                in = httpConn.getInputStream();
                Log.d("88", "HTTP_OK success");
            }
        }catch (MalformedURLException e) {
            e.printStackTrace();
        }catch (IOException e) {
            e.printStackTrace();
        }
        return in;
    }

    /*
    //close the download progress window
    private Handler messageHandler = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            progressDialog.dismiss();
        }
    };
    */
}
