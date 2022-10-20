/*
 * 8-bit Galois Field
 *
 * Copyright 2015, Backblaze, Inc.  All rights reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 *         of this software and associated documentation files (the "Software"), to deal
 *         in the Software without restriction, including without limitation the rights
 *         to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 *         copies of the Software, and to permit persons to whom the Software is
 *         furnished to do so, subject to the following conditions:
 *
 *         The above copyright notice and this permission notice shall be included in all
 *         copies or substantial portions of the Software.
 *
 *         THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *         IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *         FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *         AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *         LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *         OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 *         SOFTWARE.
 */

package org.orbisgis.sos.bandfilter;

import org.junit.Test;
import org.orbisgis.sos.SOSSignalProcessing;
import org.orbisgis.sos.WindowTest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteOrder;
import java.util.Locale;

import static org.junit.Assert.*;

public class BandFilterTest {

    @Test
    public void testFilteringSpeak() throws IOException {
        Logger logger = LoggerFactory.getLogger("testFilteringSpeak");

        InputStream inputStream = WindowTest.class.getResourceAsStream("speak_44100Hz_16bitsPCM_10s.raw");
        assertNotNull(inputStream);
        short[] signal = SOSSignalProcessing.loadShortStream(inputStream, ByteOrder.LITTLE_ENDIAN);


        short[] processed = new short[signal.length];

        long start = System.currentTimeMillis();
        BandFilter bandFilter = new BandFilter(44100, 0, 1000);
        for(int i=0; i < signal.length; i++) {
            processed[i] = (short)(32768 * bandFilter.applyBandPassFilter(signal[i] / 32768.0));
        }
        logger.info(String.format(Locale.ROOT, "Done in %d milliseconds", System.currentTimeMillis() - start));

        // write to target file
        try(OutputStream outputStream = new FileOutputStream("build/speak_44100Hz_16bitsPCM_10s_filtered.raw")) {
            SOSSignalProcessing.writeShortStream(outputStream, processed, ByteOrder.LITTLE_ENDIAN);
        }
    }
}