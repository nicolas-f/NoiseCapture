/*
 * This file is part of the NoiseCapture application and OnoMap system.
 *
 * The 'OnoMaP' system is led by Lab-STICC and Ifsttar and generates noise maps via
 * citizen-contributed noise data.
 *
 * This application is co-funded by the ENERGIC-OD Project (European Network for
 * Redistributing Geospatial Information to user Communities - Open Data). ENERGIC-OD
 * (http://www.energic-od.eu/) is partially funded under the ICT Policy Support Programme (ICT
 * PSP) as part of the Competitiveness and Innovation Framework Programme by the European
 * Community. The application work is also supported by the French geographic portal GEOPAL of the
 * Pays de la Loire region (http://www.geopal.org).
 *
 * Copyright (C) IFSTTAR - LAE and Lab-STICC – CNRS UMR 6285 Equipe DECIDE Vannes
 *
 * NoiseCapture is a free software; you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation; either version 3 of
 * the License, or(at your option) any later version. NoiseCapture is distributed in the hope that
 * it will be useful,but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for
 * more details.You should have received a copy of the GNU General Public License along with this
 * program; if not, write to the Free Software Foundation,Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301  USA or see For more information,  write to Ifsttar,
 * 14-20 Boulevard Newton Cite Descartes, Champs sur Marne F-77447 Marne la Vallee Cedex 2 FRANCE
 *  or write to scientific.computing@ifsttar.fr
 */

package org.noise_planet.noisecapture;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.wifi.p2p.WifiP2pDevice;
import android.os.Bundle;
import android.os.IBinder;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import com.peak.salut.Callbacks.SalutCallback;
import com.peak.salut.Salut;
import com.peak.salut.SalutDataReceiver;
import com.peak.salut.SalutServiceData;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class CalibrationWifiGuest extends MainActivity implements PropertyChangeListener {

    private boolean mIsBound = false;
    private CalibrationService calibrationService;
    private Salut network;
    private ImageView connectionStatusImage;
    private TextView textStatus;
    private TextView textDeviceName;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calibration_wifi_guest);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        initDrawer();

        connectionStatusImage = (ImageView) findViewById(R.id.imageView_value_wifi_state);
        textStatus = (TextView) findViewById(R.id.calibration_state);
        textDeviceName = (TextView) findViewById(R.id.calibration_host_ssid);


        if(checkAndAskWifiP2PPermissions()) {
            doBindService();
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        if(AudioProcess.PROP_DELAYED_STANDART_PROCESSING.equals(event.getPropertyName())){
            // New leq
            AudioProcess.AudioMeasureResult measure =
                    (AudioProcess.AudioMeasureResult) event.getNewValue();
            final double leq = measure.getSignalLeq();

        } else if(CalibrationService.PROP_CALIBRATION_STATE.equals(event.getPropertyName())) {
            // Calibration service state change, inform user
            CalibrationService.CALIBRATION_STATE newState =
                    (CalibrationService.CALIBRATION_STATE)event.getNewValue();
            CalibrationWifiHost.applyStateChange(newState, connectionStatusImage, textStatus);
        } else if(CalibrationService.PROP_P2P_DEVICE.equals(event.getPropertyName())) {
            /* WifiP2pDevice p2pDevice = calibrationService.getWifiP2pDevice();
            if(p2pDevice != null) {
                textDeviceName.setText(p2pDevice.deviceName);
            } else {
                textDeviceName.setText("");
            }
            */
        }
    }

    private ServiceConnection mConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder service) {
            calibrationService = ((CalibrationService.LocalBinder)service).getService();
            CalibrationWifiHost.applyStateChange(calibrationService.getState(), connectionStatusImage, textStatus);
            calibrationService.addPropertyChangeListener(CalibrationWifiGuest.this);
            CalibrationWifiGuest.this.network = new Salut(new SalutDataReceiver(CalibrationWifiGuest.this, calibrationService), new SalutServiceData(CalibrationService.SERVICE_NAME, CalibrationService.SERVICE_PORT, android.os.Build.MODEL), new SalutCallback() {
                @Override
                public void call() {
                    textStatus.setText(R.string.calibration_status_p2p_error);
                }
            });
            calibrationService.setNetwork(CalibrationWifiGuest.this.network);
            calibrationService.init();
        }

        public void onServiceDisconnected(ComponentName className) {
            // This is called when the connection with the service has been
            // unexpectedly disconnected -- that is, its process crashed.
            // Because it is running in our same process, we should never
            // see this happen.
            calibrationService.removePropertyChangeListener(CalibrationWifiGuest.this);
            calibrationService = null;
        }
    };


    void doUnbindService() {
        if (mIsBound) {
            calibrationService.removePropertyChangeListener(this);
            // Detach our existing connection.
            unbindService(mConnection);
            mIsBound = false;
        }
    }

    void doBindService() {
        // Establish a connection with the service.  We use an explicit
        // class name because we want a specific service implementation that
        // we know will be running in our own process (and thus won't be
        // supporting component replacement by other applications).
        Intent intent = new Intent(this, CalibrationService.class);
        intent.putExtra(CalibrationService.EXTRA_HOST, 0);
        if(bindService(intent, mConnection, Context.BIND_AUTO_CREATE)) {
            mIsBound = true;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        doBindService();
    }

    @Override
    protected void onResume() {
        doBindService();
        super.onPostResume();    }

    @Override
    protected void onPause() {
        // Disconnect listener from measurement
        doUnbindService();
        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Disconnect listener from measurement
        doUnbindService();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(calibrationService != null) {
            // Disconnect listener from measurement
            doUnbindService();
        }
    }

}
