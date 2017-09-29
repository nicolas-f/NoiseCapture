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

package org.noise_planet.noisecapturegs

import geoserver.GeoServer
import geoserver.catalog.Store
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.sql.Sql
import org.geotools.jdbc.JDBCDataStore
import org.springframework.security.core.context.SecurityContextHolder

import java.sql.Connection
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.time.DateTimeException
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.zone.ZoneRulesException
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

title = 'nc_dump_records'
description = 'Dump database data to a folder'

inputs = [
        exportTracks  : [name: 'exportTracks', title: 'Export raw track boolean',
                         type: Boolean.class],
        exportMeasures: [name: 'exportMeasures', title: 'Export raw measures boolean',
                         type: Boolean.class],
        exportAreas   : [name: 'exportAreas', title: 'Export post-processed values',
                         type: Boolean.class]
]

outputs = [
        result: [name: 'result', title: 'Created files', type: String.class]
]

/**
 * Convert EPOCH time to ISO 8601
 * @param epochMillisec
 * @return
 */
static def epochToRFCTime(long epochMillisec, String zone) {
    ZoneId zoneId = ZoneId.systemDefault();
    try {
        zoneId = ZoneId.of(zone)
    } catch (DateTimeException ex) {
        // skip
    } catch (ZoneRulesException ex) {
        // skip
    }
    return Instant.ofEpochMilli(epochMillisec).atZone(zoneId).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
}



static
def getDump(Connection connection, File outPath, boolean exportTracks, boolean exportMeasures, boolean exportAreas) {
    // gzip then base64 the content (http://www.txtwizard.net/compression)
    final String README_CONTENT = "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"


    def createdFiles = new ArrayList<String>()

    // Process export of raw measures
    try {
        connection.setAutoCommit(false);
        def sql = new Sql(connection)
        // Change result set type, this way the PostGIS driver use the db cursor with minimal memory usage
        sql.setResultSetType(ResultSet.TYPE_FORWARD_ONLY)
        sql.setResultSetConcurrency(ResultSet.CONCUR_READ_ONLY)
        sql.withStatement{ stmt -> stmt.fetchSize = 50 }
        // This variable host the opened connection to all files
        def countryZipOutputStream = [:]
        // Create a table that contains track envelopes
        if(exportTracks || exportMeasures) {
            sql.execute("TRUNCATE TABLE NOISECAPTURE_DUMP_TRACK_ENVELOPE")
            sql.execute("INSERT INTO NOISECAPTURE_DUMP_TRACK_ENVELOPE SELECT pk_track, " +
                    "ST_SETSRID(ST_EXTENT(ST_MAKEPOINT(ST_X(the_geom),ST_Y(the_geom))), 4326) the_geom,  COUNT(np.pk_point) measure_count" +
                    " from noisecapture_point np where not ST_ISEMPTY(the_geom)  group by pk_track")
        }
        // Loop through region level
        // Region table has been downloaded from http://www.gadm.org/version2
        def lastFileParams = []
        ZipOutputStream lastFileZipOutputStream = null
        Writer lastFileJsonWriter = null

        try {

            if (exportTracks) {
                // Export track file
                sql.eachRow("select name_0, name_1, name_2, tzid, nt.pk_track, track_uuid, pleasantness,gain_calibration,ST_AsGeoJson(te.the_geom) the_geom, record_utc, noise_level, time_length, (select string_agg(tag_name, ',') from noisecapture_tag ntag, noisecapture_track_tag nttag where ntag.pk_tag = nttag.pk_tag and nttag.pk_track = nt.pk_track) tags from noisecapture_dump_track_envelope te, gadm28 ga, noisecapture_track nt,tz_world tz  where te.the_geom && ga.the_geom and st_intersects(te.the_geom, ga.the_geom) and ga.the_geom && tz.the_geom and st_intersects(ST_PointOnSurface(ga.the_geom),tz.the_geom) and te.pk_track = nt.pk_track order by name_0, name_1, name_2, record_utc;") {
                    track_row ->
                        def thisFileParams = [track_row.name_2, track_row.name_1, track_row.name_0]
                        if (thisFileParams != lastFileParams) {
                            if (lastFileJsonWriter != null) {
                                lastFileJsonWriter << "]\n}\n"
                                lastFileJsonWriter.flush()
                                lastFileZipOutputStream.closeEntry()
                            }
                            lastFileParams = thisFileParams
                            String fileName = track_row.name_0 + "_" + track_row.name_1 + "_" + track_row.name_2 + ".tracks.geojson"
                            // Fetch ZipOutputStream for this new country
                            if (countryZipOutputStream.containsKey(track_row.name_0)) {
                                lastFileZipOutputStream = (ZipOutputStream) countryZipOutputStream.get(track_row.name_0)
                            } else {
                                // Create new file or overwrite it
                                def zipFileName = new File(outPath, track_row.name_0 + ".zip")
                                lastFileZipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
                                countryZipOutputStream.put(track_row.name_0, lastFileZipOutputStream)
                                createdFiles.add(zipFileName.getAbsolutePath())
                            }
                            lastFileZipOutputStream.putNextEntry(new ZipEntry(fileName))
                            lastFileJsonWriter = new OutputStreamWriter(lastFileZipOutputStream, "UTF-8")
                            lastFileJsonWriter << "{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n"
                        } else {
                            lastFileJsonWriter << ",\n"
                        }
                        def the_geom = new JsonSlurper().parseText(track_row.the_geom)
                        def time_ISO_8601 = epochToRFCTime(((Timestamp) track_row.record_utc).time, track_row.tzid)
                        def track = [type: "Feature", geometry: the_geom, properties: [pleasantness    : track_row.pleasantness == null ? null : (Double.isNaN(track_row.pleasantness) ? null : track_row.pleasantness),
                                                                                       pk_track        : track_row.pk_track,
                                                                                       track_uuid      : track_row.track_uuid,
                                                                                       gain_calibration: track_row.gain_calibration,
                                                                                       time_ISO8601    : time_ISO_8601,
                                                                                       time_epoch      : ((Timestamp) track_row.record_utc).time,
                                                                                       noise_level     : track_row.noise_level,
                                                                                       time_length     : track_row.time_length,
                                                                                       tags            : track_row.tags == null ? null : track_row.tags.tokenize(',')]]
                        lastFileJsonWriter << JsonOutput.toJson(track)
                }
                if (lastFileJsonWriter != null) {
                    lastFileJsonWriter << "]\n}\n"
                    lastFileJsonWriter.flush()
                    lastFileZipOutputStream.closeEntry()
                }
            }
            lastFileParams = []
            lastFileZipOutputStream = null
            lastFileJsonWriter = null

            // Export measures file
            if (exportMeasures) {
                sql.eachRow("select name_0, name_1, name_2, tzid, np.pk_track, ST_AsGeoJson(np.the_geom) the_geom, np.noise_level, np.speed, np.accuracy, np.orientation, np.time_date, np.time_location  from noisecapture_dump_track_envelope te, gadm28 ga, noisecapture_point np,tz_world tz  where te.the_geom && ga.the_geom and st_intersects(te.the_geom, ga.the_geom) and ga.the_geom && tz.the_geom and st_intersects(ST_PointOnSurface(ga.the_geom),tz.the_geom) and te.pk_track = np.pk_track and not ST_ISEMPTY(np.the_geom) order by name_0, name_1, name_2, np.time_date") {
                    track_row ->
                        def thisFileParams = [track_row.name_2, track_row.name_1, track_row.name_0]
                        if (thisFileParams != lastFileParams) {
                            if (lastFileJsonWriter != null) {
                                lastFileJsonWriter << "]\n}\n"
                                lastFileJsonWriter.flush()
                                lastFileZipOutputStream.closeEntry()
                            }
                            lastFileParams = thisFileParams
                            String fileName = track_row.name_0 + "_" + track_row.name_1 + "_" + track_row.name_2 + ".points.geojson"
                            // Fetch ZipOutputStream for this new country
                            if (countryZipOutputStream.containsKey(track_row.name_0)) {
                                lastFileZipOutputStream = (ZipOutputStream) countryZipOutputStream.get(track_row.name_0)
                            } else {
                                // Create new file or overwrite it
                                def zipFileName = new File(outPath, track_row.name_0 + ".zip")
                                lastFileZipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
                                countryZipOutputStream.put(track_row.name_0, lastFileZipOutputStream)
                                createdFiles.add(zipFileName.getAbsolutePath())
                            }
                            lastFileZipOutputStream.putNextEntry(new ZipEntry(fileName))
                            lastFileJsonWriter = new OutputStreamWriter(lastFileZipOutputStream, "UTF-8")
                            lastFileJsonWriter << "{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n"
                        } else {
                            lastFileJsonWriter << ",\n"
                        }
                        def the_geom = new JsonSlurper().parseText(track_row.the_geom)
                        def time_ISO_8601 = epochToRFCTime(((Timestamp) track_row.time_date).time, track_row.tzid)
                        def time_gps_ISO_8601 = epochToRFCTime(((Timestamp) track_row.time_location).time, track_row.tzid)
                        def track = [type: "Feature", geometry: the_geom, properties: [pk_track        : track_row.pk_track,
                                                                                                               time_ISO8601    : time_ISO_8601,
                                                                                                               time_epoch      : ((Timestamp) track_row.time_date).time,
                                                                                                               time_gps_ISO8601: time_gps_ISO_8601,
                                                                                                               time_gps_epoch  : ((Timestamp) track_row.time_location).time,
                                                                                                               noise_level     : track_row.noise_level,
                                                                                                               speed           : track_row.speed,
                                                                                                               orientation     : track_row.orientation,
                                                                                                               accuracy        : track_row.accuracy
                        ]]
                        lastFileJsonWriter << JsonOutput.toJson(track)
                }
                if (lastFileJsonWriter != null) {
                    lastFileJsonWriter << "]\n}\n"
                    lastFileJsonWriter.flush()
                    lastFileZipOutputStream.closeEntry()
                }
            }
            // Export hexagons file
            lastFileParams = []
            lastFileZipOutputStream = null
            lastFileJsonWriter = null
            if (exportAreas) {
                // Export track file
                sql.eachRow("SELECT name_0, name_1, name_2,ST_AsGeoJson(na.the_geom) the_geom, cell_q, cell_r, tzid, la50, na.laeq, lden , mean_pleasantness, measure_count, first_measure, last_measure, string_agg(to_char(nap.laeq, 'FM999.9'), '_') leq_profile, string_agg(to_char(hour, '999'), '_') hour_profile FROM noisecapture_area na, gadm28 ga, (select pk_area, nap.laeq, hour from noisecapture_area_profile nap  order by hour) nap  where ST_Centroid(na.the_geom) && ga.the_geom and st_contains(ga.the_geom, ST_centroid(na.the_geom)) and nap.pk_area = na.pk_area and na.pk_party is null group by name_0, name_1, name_2,na.the_geom, cell_q, cell_r, tzid, la50, na.laeq, lden , mean_pleasantness, measure_count, first_measure, last_measure order by name_0, name_1, name_2, cell_q, cell_r;") {
                    track_row ->
                        def thisFileParams = [track_row.name_2, track_row.name_1, track_row.name_0]
                        if (thisFileParams != lastFileParams) {
                            if (lastFileJsonWriter != null) {
                                lastFileJsonWriter << "]\n}\n"
                                lastFileJsonWriter.flush()
                                lastFileZipOutputStream.closeEntry()
                            }
                            lastFileParams = thisFileParams
                            String fileName = track_row.name_0 + "_" + track_row.name_1 + "_" + track_row.name_2 + ".areas.geojson"
                            // Fetch ZipOutputStream for this new country
                            if (countryZipOutputStream.containsKey(track_row.name_0)) {
                                lastFileZipOutputStream = (ZipOutputStream) countryZipOutputStream.get(track_row.name_0)
                            } else {
                                // Create new file or overwrite it
                                def zipFileName = new File(outPath, track_row.name_0 + ".zip")
                                lastFileZipOutputStream = new ZipOutputStream(new FileOutputStream(zipFileName));
                                countryZipOutputStream.put(track_row.name_0, lastFileZipOutputStream)
                                createdFiles.add(zipFileName.getAbsolutePath())
                            }
                            lastFileZipOutputStream.putNextEntry(new ZipEntry(fileName))
                            lastFileJsonWriter = new OutputStreamWriter(lastFileZipOutputStream, "UTF-8")
                            lastFileJsonWriter << "{\n  \"type\": \"FeatureCollection\",\n  \"features\": [\n"
                        } else {
                            lastFileJsonWriter << ",\n"
                        }
                        def first_measure_ISO_8601 = epochToRFCTime(((Timestamp) track_row.first_measure).time, track_row.tzid)
                        def last_measure_ISO_8601 = epochToRFCTime(((Timestamp) track_row.last_measure).time, track_row.tzid)

                        def the_geom = new JsonSlurper().parseText(track_row.the_geom)

                        def leq_keys= track_row.hour_profile.tokenize('_')*.toInteger()
                        def leq_values= track_row.leq_profile.tokenize('_')*.toFloat()
                        def leq_array = new Object[72]
                        [leq_keys, leq_values].transpose().each {leq_array[it[0]] = it[1]}
                        def track = [type: "Feature", geometry: [type: "Polygon", coordinates: the_geom.coordinates], properties: [cell_q                : track_row.cell_q,
                                                                                                                                  cell_r                : track_row.cell_r,
                                                                                                                                  la50                  : track_row.la50,
                                                                                                                                  laeq                  : track_row.laeq,
                                                                                                                                  lden                  : track_row.lden,
                                                                                                                                  mean_pleasantness     : track_row.mean_pleasantness == null ? null : (Double.isNaN(track_row.mean_pleasantness) ? null : track_row.mean_pleasantness),
                                                                                                                                  measure_count         : track_row.measure_count,
                                                                                                                                  first_measure_ISO_8601: first_measure_ISO_8601,
                                                                                                                                  first_measure_epoch   : ((Timestamp) track_row.first_measure).time,
                                                                                                                                  last_measure_ISO_8601 : last_measure_ISO_8601,
                                                                                                                                  last_measure_epoch    : ((Timestamp) track_row.last_measure).time,
                                                                                                                                  leq_profile           : leq_array]]
                        lastFileJsonWriter << JsonOutput.toJson(track)
                }
                if (lastFileJsonWriter != null) {
                    lastFileJsonWriter << "]\n}\n"
                    lastFileJsonWriter.flush()
                    lastFileZipOutputStream.closeEntry()
                }
            }
        } finally {
            // Close opened files
            countryZipOutputStream.each {
                k, v ->
                    v.closeEntry()
                    // Write readme file
                    v.putNextEntry(new ZipEntry("README.html"))
                    new ByteArrayInputStream(Base64.decoder.decode(README_CONTENT)).withStream {
                        bais ->
                            new GZIPInputStream(bais).withStream {
                                html ->
                                    v <<  html;
                            }
                    }
                    v.closeEntry()
                    // Close zip file stream
                    v.close()
            }
        }
    } catch (SQLException ex) {
        throw ex
    }
    return createdFiles
}

static def Connection openPostgreSQLDataStoreConnection() {
    Store store = new GeoServer().catalog.getStore("postgis")
    JDBCDataStore jdbcDataStore = (JDBCDataStore) store.getDataStoreInfo().getDataStore(null)
    return jdbcDataStore.getDataSource().getConnection()
}

def run(input) {
    // Sensible WPS process, add a layer of security by checking for authenticated user
    def user = SecurityContextHolder.getContext().getAuthentication();
    if (!user.isAuthenticated()) {
        throw new IllegalStateException("This WPS process require authentication")
    }
    // Open PostgreSQL connection
    // Create dump folder
    File dumpDir = new File("data_dir/onomap_public_dump");
    if (!dumpDir.exists()) {
        dumpDir.mkdirs()
    }
    Connection connection = openPostgreSQLDataStoreConnection()
    try {
        return [result: JsonOutput.toJson(getDump(connection, dumpDir,input["exportTracks"], input["exportMeasures"], input["exportAreas"]))]
    } finally {
        connection.close()
    }
}
