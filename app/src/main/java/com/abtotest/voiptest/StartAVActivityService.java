/**
 * 
 */
package com.abtotest.voiptest;

import android.app.IntentService;
import android.content.Intent;

/**
 * @author "Volodymyr Kurniavka"
 *
 */
public class StartAVActivityService extends IntentService {

    public StartAVActivityService() {
        super("StartAVService");
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see android.app.IntentService#onHandleIntent(android.content.Intent)
     */
    @Override
    protected void onHandleIntent(Intent intent) {
        Intent activityIntent = new Intent(intent);
        
        activityIntent.setClass(this, ScreenAV.class);
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(activityIntent);

    }

}
