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

import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.util.JsonReader;
import android.util.JsonToken;

import junit.framework.Assert;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * Unit test of SQLLite db manager of NoiseCapture
 */
@RunWith(RobolectricTestRunner.class)
@Config(constants = BuildConfig.class, sdk = 23)
public class TestDB {

    @Rule
    public TemporaryFolder folder= new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        folder.create();
    }

    @Test
    public void testCreate() throws URISyntaxException {
        MeasurementManager measurementManager =
                new MeasurementManager(RuntimeEnvironment.application);

        int recordId = measurementManager.addRecord();
        Storage.Leq leq = new Storage.Leq(recordId, -1, System.currentTimeMillis(), 12, 15, 50.d,
                15.f, 4.f, 4.5f,System.currentTimeMillis());
        List<Storage.LeqValue> leqValues = new ArrayList<Storage.LeqValue>();

        leqValues .add(new Storage.LeqValue(-1, 125, 65));
        leqValues .add(new Storage.LeqValue(-1, 250, 55));
        leqValues .add(new Storage.LeqValue(-1, 500, 56));
        leqValues .add(new Storage.LeqValue(-1, 1000, 58));
        leqValues .add(new Storage.LeqValue(-1, 2000, 48));
        leqValues .add(new Storage.LeqValue(-1, 4000, 49));
        leqValues .add(new Storage.LeqValue(-1, 8000, 45));
        leqValues .add(new Storage.LeqValue(-1, 16000, 41));
        MeasurementManager.LeqBatch leqBatch = new MeasurementManager.LeqBatch(leq,leqValues);
        measurementManager.addLeqBatch(leqBatch);
        leq = new Storage.Leq(recordId, -1, System.currentTimeMillis(), 12.01, 15.02, 51.d,
                12.f, 3.02f, 5f,System.currentTimeMillis());
        leqBatch = new MeasurementManager.LeqBatch(leq,leqValues);
        measurementManager.addLeqBatch(leqBatch);

        // Fetch data
        List<MeasurementManager.LeqBatch> storedLeq =
                measurementManager.getRecordLocations(recordId, true, 0);
        assertEquals(2, storedLeq.size());
        MeasurementManager.LeqBatch checkLeq = storedLeq.remove(0);
        assertEquals(15, checkLeq.getLeq().getLongitude(), 0.01);
        assertEquals(12, checkLeq.getLeq().getLatitude(), 0.01);
        assertEquals(50, checkLeq.getLeq().getAltitude(), 0.01);
        assertEquals(15.f, checkLeq.getLeq().getSpeed(), 0.01);
        assertEquals(4.f, checkLeq.getLeq().getBearing(), 0.01);
        checkLeq = storedLeq.remove(0);
        assertEquals(15.02, checkLeq.getLeq().getLongitude(), 0.01);
        assertEquals(12.01, checkLeq.getLeq().getLatitude(), 0.01);
        assertEquals(51, checkLeq.getLeq().getAltitude(), 0.01);
        assertEquals(12.f, checkLeq.getLeq().getSpeed(), 0.01);
        assertEquals(3.02f, checkLeq.getLeq().getBearing(), 0.01);

        // Check update ending measure

        measurementManager.updateRecordFinal(recordId, (float)leqBatch.computeGlobalLeq(), 2, 2.31f);


        Storage.Record incompleteRecord = measurementManager.getRecord(recordId);
        assertNull(incompleteRecord.getPleasantness());
        assertEquals((float)leqBatch.computeGlobalLeq(), incompleteRecord.getLeqMean(), 0.01);
        assertEquals(2.31f, incompleteRecord.getCalibrationGain(), 0.01);
        Assert.assertNull(incompleteRecord.getNoisePartyTag());

        // Check update user input
        measurementManager.updateRecordUserInput(recordId, "This is a description",
                (short)2,new String[]{Storage.TAGS_INFO[0].name, Storage.TAGS_INFO[4].name},
                Uri.fromFile(new File(TestDB.class.getResource("calibration.png").getFile())),
                "OGRS2018");
        Storage.Record record = measurementManager.getRecord(recordId);
        assertEquals(Uri.fromFile(new File(TestDB.class.getResource("calibration.png").getFile())),
                record.getPhotoUri());

        List<String> selectedTags = measurementManager.getTags(recordId);
        assertEquals(Storage.TAGS_INFO[0].name, selectedTags.get(0));
        assertEquals(Storage.TAGS_INFO[4].name, selectedTags.get(1));
        assertEquals("OGRS2018", record.getNoisePartyTag());

    }

    @Test
    public void testResults() throws URISyntaxException {
        MeasurementManager measurementManager =
                new MeasurementManager(RuntimeEnvironment.application);

        int recordId = measurementManager.addRecord();
        Storage.Leq leq = new Storage.Leq(recordId, -1, System.currentTimeMillis(), 12, 15, 50.d,
                15.f, 4.f, 4.5f,System.currentTimeMillis());
        List<Storage.LeqValue> leqValues = new ArrayList<Storage.LeqValue>();

        leqValues .add(new Storage.LeqValue(-1, 125, 65));
        leqValues .add(new Storage.LeqValue(-1, 250, 55));
        leqValues .add(new Storage.LeqValue(-1, 500, 56));
        leqValues .add(new Storage.LeqValue(-1, 1000, 58));
        leqValues .add(new Storage.LeqValue(-1, 2000, 48));
        leqValues .add(new Storage.LeqValue(-1, 4000, 49));
        leqValues .add(new Storage.LeqValue(-1, 8000, 45));
        leqValues .add(new Storage.LeqValue(-1, 16000, 41));
        MeasurementManager.LeqBatch leqBatch = new MeasurementManager.LeqBatch(leq,leqValues);
        measurementManager.addLeqBatch(leqBatch);
        leq = new Storage.Leq(recordId, -1, System.currentTimeMillis(), 12.01, 15.02, 51.d,
                12.f, 3.02f, 5f,System.currentTimeMillis());
        leqBatch = new MeasurementManager.LeqBatch(leq,leqValues);
        measurementManager.addLeqBatch(leqBatch);

        // Fetch data
        List<Integer> frequency = new ArrayList<>();
        List<Float[]> leqs = new ArrayList<>();
        assertTrue(measurementManager.getRecordLeqs(recordId, frequency, leqs, null));
        assertEquals(2, leqs.size());
        Float[] checkLeq = leqs.remove(0);
        Assert.assertNotNull(checkLeq);
        assertEquals(65, checkLeq[0], 0.1);
        assertEquals(55, checkLeq[1], 0.1);
        assertEquals(56, checkLeq[2], 0.1);
        assertEquals(58, checkLeq[3], 0.1);
        assertEquals(48, checkLeq[4], 0.1);
        assertEquals(49, checkLeq[5], 0.1);
        assertEquals(45, checkLeq[6], 0.1);
        assertEquals(41, checkLeq[7], 0.1);

        checkLeq = leqs.remove(0);
        assertEquals(65, checkLeq[0], 0.1);
        assertEquals(55, checkLeq[1], 0.1);
        assertEquals(56, checkLeq[2], 0.1);
        assertEquals(58, checkLeq[3], 0.1);
        assertEquals(48, checkLeq[4], 0.1);
        assertEquals(49, checkLeq[5], 0.1);
        assertEquals(45, checkLeq[6], 0.1);
        assertEquals(41, checkLeq[7], 0.1);
    }
    @Test
    public void testExport() throws URISyntaxException, IOException {
        MeasurementManager measurementManager =
                new MeasurementManager(RuntimeEnvironment.application);

        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(RuntimeEnvironment.application);
        assertTrue(sharedPref.edit().putString("settings_user_noise_knowledge", "NOVICE").commit());

        int recordId = measurementManager.addRecord();
        Storage.Leq leq = new Storage.Leq(recordId, -1, System.currentTimeMillis(), 12, 15, 50.d,
                15.f, 4.f, 4.5f,System.currentTimeMillis());
        List<Storage.LeqValue> leqValues = new ArrayList<Storage.LeqValue>();

        leqValues .add(new Storage.LeqValue(-1, 125, 65));
        leqValues .add(new Storage.LeqValue(-1, 250, 55));
        leqValues .add(new Storage.LeqValue(-1, 500, 56));
        leqValues .add(new Storage.LeqValue(-1, 1000, 58));
        leqValues .add(new Storage.LeqValue(-1, 2000, 48));
        leqValues .add(new Storage.LeqValue(-1, 4000, 49));
        leqValues .add(new Storage.LeqValue(-1, 8000, 45));
        leqValues .add(new Storage.LeqValue(-1, 16000, 41));
        MeasurementManager.LeqBatch leqBatch = new MeasurementManager.LeqBatch(leq,leqValues);
        measurementManager.addLeqBatch(leqBatch);
        leq = new Storage.Leq(recordId, -1, System.currentTimeMillis(), 12.01, 15.02, 51.d,
                12.f, 3.02f, 5f,System.currentTimeMillis());
        leqBatch = new MeasurementManager.LeqBatch(leq,leqValues);
        measurementManager.addLeqBatch(leqBatch);

        measurementManager.updateRecordFinal(recordId, (float)leqBatch.computeGlobalLeq(), 2, -4.76f);
        measurementManager.updateRecordUserInput(recordId, "This is a description",
                (short)2,new String[]{Storage.TAGS_INFO[0].name, Storage.TAGS_INFO[4].name},
                Uri.fromFile(new File(TestDB.class.getResource("calibration.png").getFile())),
                "OGRS2018");

        // Export to zip file
        File testFile = folder.newFile("testexport.zip");
        History.doBuildZip(testFile, RuntimeEnvironment.application, recordId);

        // Check properties of zip file
        FileInputStream fileInputStream = new FileInputStream(testFile);
        Properties meta = null;
        boolean foundJson = false;
        try {
            ZipInputStream zipInputStream = new ZipInputStream(fileInputStream);
            ZipEntry zipEntry;
            while((zipEntry = zipInputStream.getNextEntry()) != null) {
                if (MeasurementExport.PROPERTY_FILENAME.equals(zipEntry.getName())) {
                    meta = new Properties();
                    meta.load(zipInputStream);
                }else if (MeasurementExport.GEOJSON_FILENAME.equals(zipEntry.getName())) {
                    JsonReader jsonReader = new JsonReader(new InputStreamReader(zipInputStream,
                            "UTF-8"));
                    assertTrue(jsonReader.hasNext());
                    assertEquals(JsonToken.BEGIN_OBJECT, jsonReader.peek());
                    jsonReader.beginObject();
                    assertEquals(JsonToken.NAME, jsonReader.peek());
                    Assert.assertEquals("type" ,jsonReader.nextName());
                    assertEquals(JsonToken.STRING, jsonReader.peek());
                    Assert.assertEquals("FeatureCollection" ,jsonReader.nextString());
                    assertEquals(JsonToken.NAME, jsonReader.peek());
                    Assert.assertEquals("features" ,jsonReader.nextName());
                    assertEquals(JsonToken.BEGIN_ARRAY, jsonReader.peek());
                    foundJson = true;
                }
            }
        } finally {
            fileInputStream.close();
        }
        assertTrue(foundJson);
        assertNotNull(meta);
        assertNotNull(meta.getProperty(MeasurementExport.PROP_GAIN_CALIBRATION));
        Assert.assertEquals("NOVICE", meta.getProperty(MeasurementExport.PROP_USER_PROFILE));
        Assert.assertEquals("OGRS2018", meta.getProperty(Storage.Record.COLUMN_NOISEPARTY_TAG));
        assertEquals(-4.76f, Float.valueOf(meta.getProperty(MeasurementExport.PROP_GAIN_CALIBRATION)), 0.01f);
        assertEquals((float)leqBatch.computeGlobalLeq(),
                Float.valueOf(meta.getProperty(Storage.Record.COLUMN_LEQ_MEAN)), 0.01f);
    }

    @Test
    public void testSubSample() throws IOException {
        Storage storage =  new Storage(RuntimeEnvironment.application);
        // Add dump data
        SQLiteDatabase db = storage.getWritableDatabase();
        // Reseting Counter

        // Open the resource
        InputStream insertsStream = TestDB.class.getResourceAsStream("dump.sql");
        BufferedReader insertReader = new BufferedReader(new InputStreamReader(insertsStream));

        // Iterate through lines
        while (insertReader.ready()) {
            String insertStmt = insertReader.readLine();

            try {
                db.execSQL(insertStmt);
            } catch (SQLException ex) {
                System.err.println("Error while running " + insertStmt);
                throw ex;
            }
        }
        insertReader.close();

        MeasurementManager measurementManager =
                new MeasurementManager(RuntimeEnvironment.application);

        // Fetch data
        List<MeasurementManager.LeqBatch> storedLeq =
                measurementManager.getRecordLocations(29, true, 25);

        assertTrue(storedLeq.size() <= 25);
    }

}
