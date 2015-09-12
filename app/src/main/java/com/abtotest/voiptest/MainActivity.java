package com.abtotest.voiptest;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.OnIncomingCallListener;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.sdk.OnRegistrationListener;
import org.abtollc.utils.Log;
import org.abtollc.utils.codec.Codec;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.RemoteException;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;
import com.technohood.acticonnect.R;

public class MainActivity extends Activity{

    private boolean registered = false;
    private boolean isCaling = false;
    private String activeRemoteContact;
    private String domain;
    private long accId;
    private AbtoPhone abtoPhone;
    
    private Button mainButton;
    private EditText callUri;
    
    private LinearLayout allVideoLayout;
    
    private int callId;
    private ProgressDialog registrationWaiting;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        // Get AbtoPhone instance
        abtoPhone = ((AbtoApplication)getApplication()).getAbtoPhone();
        
        // show waiting dialog when initialize abtoPhone, and close it when initialized        
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setCancelable(false);
        dialog.setMessage("Loading...");
        dialog.setCancelable(false);
        
        if (abtoPhone.getActiveCallId() != AbtoPhone.INVALID_CALL_ID) {
            startAV(false);
        }
        
        //set listener what call when service is initialized. First configuration may take more time.
        abtoPhone.setInitializeListener(new OnInitializeListener() {
            @Override
            public void onInitializeState(OnInitializeListener.InitializeState state, String message) {
                switch (state) {
                case START:
                    if(!dialog.isShowing())
                        dialog.show();
                    break;
                case INFO:
                case WARNING:
                    break;
                case FAIL:
                    dialog.dismiss();
                    new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Ok", new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();

                                }
                            }).create().show();
                    break;
                case SUCCESS:
                   dialog.dismiss();
                    break;

                default:
                    break;
                }
            }
        });

        abtoPhone.getConfig().setCodecPriority(Codec.G729, (short)250);
        abtoPhone.getConfig().setCodecPriority(Codec.PCMU, (short)200);
        abtoPhone.getConfig().setCodecPriority(Codec.GSM, (short)150);
        abtoPhone.getConfig().setCodecPriority(Codec.PCMA, (short)100);
        abtoPhone.getConfig().setCodecPriority(Codec.speex_16000, (short)50);
        Log.setLogLevel(5);
        Log.setUseFile(true);
        
        // Start initialize
        abtoPhone.initialize();
        if (!abtoPhone.isActive()) {
            dialog.show();
        }// initialization
        
        mainButton = (Button) findViewById(R.id.main_button);
        callUri = (EditText) findViewById(R.id.sip_number);
        
        registrationWaiting = new ProgressDialog(this);
        registrationWaiting.setMessage("Registration in progress");
        registrationWaiting.setCancelable(false);
        
        final RegisterDialog regDialog = new RegisterDialog(this);
        
        if(!registered){
            mainButton.setText("Register");
            callUri.setEnabled(false);
        }
        
        if(isCaling){
            mainButton.setText("Hangup");
            callUri.setEnabled(false);
        }
        
        // Set the action depending on registered or calling
        mainButton.setOnClickListener(new View.OnClickListener() {
            
            public void onClick(View v) {
                if(!registered){
                    //Show registration dialog
                    regDialog.show();
                }else if(!isCaling){
                    String sipNumber = callUri.getText().toString();
                    if(sipNumber != null && !sipNumber.equals("")){
                     // Start Call
                        try {
                            abtoPhone.startVideoCall(sipNumber, accId);
                            callId = abtoPhone.getActiveCallId();
                            startAV(false);
                            if(!sipNumber.contains("@")){
                                activeRemoteContact = String.format("<sip:%1$s@%2$s>", sipNumber, domain);
                            }else{
                                activeRemoteContact = String.format("<sip:%1$s>", sipNumber);
                            }
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                    }
                }                
                
            }
        });// mainButton onClicListener
        
        // Set registration event
        abtoPhone.setRegistrationStateListener(new OnRegistrationListener() {
            
            public void onRegistrationFailed(long accId, int statusCode, String statusText) {
                registered = false; 
                domain = null;
                registrationWaiting.dismiss();
                AlertDialog.Builder fail = new AlertDialog.Builder(MainActivity.this);
                fail.setTitle("Registration Faild");
                fail.setMessage(statusCode + " - " + statusText);
                fail.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();                                             
                    }
                });
                
                fail.show();
                
            }
            
            public void onRegistered(long accId) {
              mainButton.setText("Call");
              callUri.setEnabled(true);
              registered = true;
              registrationWaiting.dismiss();
              
            }

            @Override
            public void onUnRegistered(long arg0) {
                // TODO Auto-generated method stub
                
            }
        }); //registration listener
        
        // Set on Incoming call listener
        abtoPhone.setIncomingCallListener(new OnIncomingCallListener() {
            
            public void OnIncomingCall(String remoteContact, long accountId) {
            	activeRemoteContact = remoteContact;
            	callId = abtoPhone.getActiveCallId();
            	startAV(true);
            }
        }); //incoming call listener
        
    }

    @Override
    protected void onResume() {
    	if (abtoPhone.getActiveCallId() != AbtoPhone.INVALID_CALL_ID) {
            startAV(false);
        }
    	
    	super.onResume();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_main, menu);
        return true;
    }
    private class RegisterDialog extends Dialog{
        public RegisterDialog(Context context){
            super(context);
            setContentView(R.layout.register_dialog);
            
            Button regButton = (Button) findViewById(R.id.register_button);
            final EditText user = (EditText) findViewById(R.id.login);
            final EditText pass = (EditText) findViewById(R.id.password);
            final EditText domain = (EditText) findViewById(R.id.domain);
            
            regButton.setOnClickListener(new View.OnClickListener() {
                
                public void onClick(View v) {
                    // Add account
                    accId = abtoPhone.getConfig().addAccount(domain.getText().toString(), user.getText().toString(), pass.getText().toString(), null, user.getText().toString(), 300, true);
                    //And register added account
                    MainActivity.this.domain = domain.getText().toString();
                    try {
                        abtoPhone.register();
                    } catch (RemoteException e) {
                        e.printStackTrace();
                    }
                    if(registrationWaiting != null){
                        registrationWaiting.show();
                    }
                    RegisterDialog.this.dismiss();
                }
            });
        }
    }

    private synchronized Intent prepareIntent(boolean incoming) {
        Intent intent = new Intent(this, StartAVActivityService.class);
        intent.putExtra("incoming", incoming);
        intent.putExtra(ScreenAV.CALL_ID, callId);
        intent.putExtra(AbtoPhone.REMOTE_CONTACT, activeRemoteContact);

        return intent;
    }

    private synchronized void startAV(boolean incoming) {
        Intent intent = prepareIntent(incoming);
        startService(intent);
    }
}
