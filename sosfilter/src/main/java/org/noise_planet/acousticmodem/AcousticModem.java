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
package org.noise_planet.acousticmodem;

import org.orbisgis.sos.AcousticIndicators;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.Comparator;

/**
 * Acoustic modem.
 */
public class AcousticModem {
    private Settings settings;

    public static final int CRC_SIZE = Short.SIZE / Byte.SIZE;
    private ByteBuffer crcBuffer = ByteBuffer.allocate(CRC_SIZE);

    // Used to compute background noise
    private static final int BACKGROUND_LEQ_CAPACITY = 100;
    private float[][] levelsHisto = new float[8][];

    // ReedSolomon parameters
    public static final int DATA_SHARDS = 4;
    public static final int PARITY_SHARDS = 2;

    private int lastInputWordCount = 0;
    private Integer lastInputWord = null;
    private int pushedSpectrumCount = 0;

    private Integer ignoreDuplicateWord = Integer.MAX_VALUE;

    // Number of blank word necessary to clean the word cache
    private static final int FORGET_EXPIRED_WORD = 4;

    public AcousticModem(Settings settings) {
        this.settings = settings;
        for(int i=0; i < settings.frequencies.length; i++) {
            levelsHisto[i] = new float[0];
        }
    }


    public Settings getSettings() {
        return settings;
    }

    private void copyTone(int wordId, short[] out, int outIndex, short toneRms) {
        int freqA = settings.words[wordId][0];
        int freqB = settings.words[wordId][1];
        Arrays.fill(out, outIndex, outIndex + settings.wordLength, (short)0);
        int signalLength = settings.wordLength / 2;
        int offset = signalLength / 2;
        for (int s = 0; s < signalLength; s++) {
            double t = s * (1 / (double) settings.samplingRate);
            double firstSin = Math.sin(2 * Math.PI * freqA * t) * (toneRms);
            double secondSin = Math.sin(2 * Math.PI * freqB * t) * (toneRms);
            double window = 0.5 * (1 - Math.cos((2 * Math.PI * s) / (signalLength)));
            out[outIndex + offset + s] = (short) ((firstSin + secondSin) * window);
        }
    }

    public byte[] encode(byte[] in) throws IOException {
        crcBuffer.clear();
        crcBuffer.putShort(0, crc16(in, 0, in.length));
        byte[] out = new byte[in.length + CRC_SIZE];
        System.arraycopy(in, 0, out, 0, in.length);
        System.arraycopy(crcBuffer.array(), 0, out, in.length, CRC_SIZE);
        return out;
    }

    private static int crc16_update(int crc, byte a) {
        crc ^= ((a+128) & 0xff);
        for (int i = 0; i < 8; ++i) {
            if ((crc & 1) != 0) {
                crc = ((crc >>> 1) ^ 0xA001) & 0xffff;
            }
            else {
                crc = (crc >>> 1) & 0xffff;
            }
        }
        return crc;
    }

    private static short crc16(byte[] bytes, int from, int to) {
        int crc = 0;
        for (int i = from; i < to; i++) {
            crc = crc16_update(crc, bytes[i]);
        }
        return (short) crc;
    }

    /**
     * @param in
     * @return True if the embedded crc32 code at the end of the message check the beginning of the message
     */
    public boolean isMessageCheck(byte[] in) {
        short signature = crc16(in, 0, in.length - CRC_SIZE);
        crcBuffer.clear();
        crcBuffer.put(Arrays.copyOfRange(in, in.length - CRC_SIZE, in.length), 0, CRC_SIZE);
        crcBuffer.position(0);
        return signature == crcBuffer.getShort(0);
    }

    public byte[] decode(byte[] in) {
        if(in.length >= CRC_SIZE) {
            return Arrays.copyOfRange(in, 0, in.length - CRC_SIZE);
        } else {
            return in;
        }
    }

    public static int[] byteToWordsIndex(byte data) {
        int v = data & 0xFF;
        return new int[] {v >>> 4, v & 0x0F};
    }

    public static byte wordsToByte(int wordA, int wordB) {
        return (byte)(wordA << 4 | wordB & 0x0F);
    }

    /**
     * Compute size of signal length for the provided data
     *
     * @param source       Source data
     * @param sourceIndex  First location to read in source data
     * @param sourceLength Read this size from source data
     * @return size of signal length for the provided data
     */
    public int getSignalLength(byte[] source, int sourceIndex, int sourceLength) {
        // Each byte take two words and each word have a blank word before
        return sourceLength * settings.wordLength * 2 * 2;
    }

    /**
     * Converts each byte into two words (hexa) then write the signal in the out array.
     *
     * @param source       Source data
     * @param sourceIndex  First location to read in source data
     * @param sourceLength Read this size from source data
     * @param out          Out array. The size must be greater than outIndex + ((sourceLength + 1) * 2 * settings.wordLength)
     * @param outIndex     Write words signal onto out beginning with this position
     * @param toneRms      Power of words signal output
     * @throws IllegalArgumentException If array size does not fit with parameters
     */
    public void wordsToSignal(byte[] source, int sourceIndex, int sourceLength, short[] out, int outIndex, short toneRms) throws IllegalArgumentException {
        if (sourceIndex + sourceLength > source.length) {
            throw new IllegalArgumentException("Source buffer length is " + source.length + " but request " + (sourceIndex + sourceLength - 1));
        }
        if (outIndex + getSignalLength(source, sourceIndex, sourceLength) > out.length) {
            throw new IllegalArgumentException("Output buffer length is too short" + out.length);
        }
        for (int idByte = sourceIndex; idByte < sourceIndex + sourceLength; idByte++) {
            int[] words = byteToWordsIndex(source[idByte]);
            // Add a blank before
            for (int i = outIndex; i < outIndex + settings.wordLength; i++) {
                out[i] = 0;
            }
            outIndex += settings.wordLength;
            copyTone(words[0], out, outIndex, toneRms);
            outIndex += settings.wordLength;
            // Add a blank before
            for (int i = outIndex; i < outIndex + settings.wordLength; i++) {
                out[i] = 0;
            }
            outIndex += settings.wordLength;
            copyTone(words[1], out, outIndex, toneRms);
            outIndex += settings.wordLength;
        }
    }

    /**
     * Try to find background noise for each frequency bands and remote it before returning the noise level.
     * @param spectrum SPL per frequency band
     * @return SPL per frequency band (without background noise)
     */
    public float[] filterSpectrum(float[] spectrum) {
        float[] ret = new float[spectrum.length];
        int idFreq = 0;
        for(float level : spectrum) {
            // Compute median level
            float medianLevel = AcousticIndicators.medianApprox(levelsHisto[idFreq]);
            if(!Float.isNaN(medianLevel)) {
                ret[idFreq] = level - medianLevel;
            } else {
                ret[idFreq] = 0;
            }
            idFreq++;
        }
        // Push spectrum to history
        for(int i = 0; i < levelsHisto.length; i++) {
            if(levelsHisto[i].length < BACKGROUND_LEQ_CAPACITY) {
                float[] oldLevels = levelsHisto[i];
                levelsHisto[i] = new float[oldLevels.length + 1];
                System.arraycopy(oldLevels, 0, levelsHisto[i], 0, oldLevels.length);
                levelsHisto[i][oldLevels.length] = spectrum[i];
            } else {
                float[] oldLevels = levelsHisto[i];
                System.arraycopy(oldLevels, 1, levelsHisto[i], 0, oldLevels.length - 1);
                levelsHisto[i][BACKGROUND_LEQ_CAPACITY - 1] = spectrum[i];
            }
        }
        return ret;
    }

    /**
     * Convert spectrum to byte.
     * @param spectrum Spectrum SPL as specified in {@link Settings#frequencies}
     * @return Byte from spectrum or null if not a new word
     */
    public Byte spectrumToWord(float[] spectrum) {
        pushedSpectrumCount++;
        if(lastInputWord != null && pushedSpectrumCount - lastInputWordCount >
                FORGET_EXPIRED_WORD) {
            lastInputWordCount = pushedSpectrumCount;
            lastInputWord = null;
        }
        // Sort values by power
        Integer[] indexes = new Integer[spectrum.length];
        for(int i=0; i < spectrum.length; i++) {
            indexes[i] = i;
        }
        Arrays.sort(indexes, new ArrayIndexComparator(spectrum));
        // If the third highest frequency bands level is inferior than second of SignalToNoiseLevel
        if(spectrum[indexes[indexes.length - 2]] - spectrum[indexes[indexes.length - 3]] > settings.getMinimalSignalToNoiseLevel()) {
            Integer word = settings.getWordFromFrequencyTuple(settings.frequencies[indexes[indexes.length - 1]], settings.frequencies[indexes[indexes.length - 2]]);
            if(word != null && (!word.equals(ignoreDuplicateWord))) {
                ignoreDuplicateWord = word;
                if(lastInputWord != null) {
                    byte val = wordsToByte(lastInputWord, word);
                    lastInputWord = null;
                    return val;
                } else {
                    lastInputWord = word;
                    lastInputWordCount = pushedSpectrumCount;
                    return null;
                }
            } else {
                lastInputWordCount = pushedSpectrumCount;
                return null;
            }
        } else {
            ignoreDuplicateWord = Integer.MAX_VALUE;
            return null;
        }
    }


    private static class ArrayIndexComparator implements Comparator<Integer> {
        float[] source;

        public ArrayIndexComparator(float[] source) {
            this.source = source;
        }

        @Override
        public int compare(Integer left, Integer right) {
            return Float.valueOf(source[left]).compareTo(source[right]);
        }
    }
}
