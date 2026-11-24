package com.example.pocketkit;

import com.example.pocketkit.tools.ImageCompressHelper;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ImageCompressHelperTest {

    @Test
    public void computeInSampleSize_handlesFitAndCropCaps() {
        assertEquals(1, ImageCompressHelper.computeInSampleSize(0, 1000, 1280, false));
        assertEquals(2, ImageCompressHelper.computeInSampleSize(4000, 3000, 1280, false));
        assertEquals(8, ImageCompressHelper.computeInSampleSize(6000, 4500, 472, true));
        assertEquals(2, ImageCompressHelper.computeInSampleSize(6000, 600, 472, true));
    }

    @Test
    public void computeFitDimensions_preservesAspectAndAvoidsUpscale() {
        ImageCompressHelper.Size small = ImageCompressHelper.computeFitDimensions(800, 600, 1280, 960);
        assertEquals(800, small.width);
        assertEquals(600, small.height);

        ImageCompressHelper.Size landscape = ImageCompressHelper.computeFitDimensions(4000, 2000, 1280, 960);
        assertEquals(1280, landscape.width);
        assertEquals(640, landscape.height);

        ImageCompressHelper.Size portrait = ImageCompressHelper.computeFitDimensions(2000, 4000, 1280, 960);
        assertEquals(640, portrait.width);
        assertEquals(1280, portrait.height);
    }

    @Test
    public void computeCenterCropRect_cropsCenteredToThreeByFour() {
        ImageCompressHelper.CropRect wideCrop = ImageCompressHelper.computeCenterCropRect(1600, 1200, 3, 4);
        assertEquals(900, wideCrop.width);
        assertEquals(1200, wideCrop.height);
        assertEquals(350, wideCrop.left);
        assertEquals(0, wideCrop.top);

        ImageCompressHelper.CropRect tallCrop = ImageCompressHelper.computeCenterCropRect(900, 2000, 3, 4);
        assertEquals(900, tallCrop.width);
        assertEquals(1200, tallCrop.height);
        assertEquals(0, tallCrop.left);
        assertEquals(400, tallCrop.top);

        ImageCompressHelper.CropRect exactCrop = ImageCompressHelper.computeCenterCropRect(354, 472, 3, 4);
        assertEquals(354, exactCrop.width);
        assertEquals(472, exactCrop.height);
        assertEquals(0, exactCrop.left);
        assertEquals(0, exactCrop.top);
    }

    @Test
    public void pickBestQuality_binarySearchesHighestFittingQuality() {
        ImageCompressHelper.EncodedResult topFits = ImageCompressHelper.pickBestQuality(
                20, 90, 100_000, q -> new byte[q * 1_000]
        );
        assertTrue(topFits.reachedTarget);
        assertEquals(90, topFits.quality);

        ImageCompressHelper.EncodedResult searchResult = ImageCompressHelper.pickBestQuality(
                20, 90, 67_500, q -> new byte[q * 1_000]
        );
        assertTrue(searchResult.reachedTarget);
        assertEquals(67, searchResult.quality);
        assertEquals(67_000, searchResult.bytes.length);

        ImageCompressHelper.EncodedResult overBudget = ImageCompressHelper.pickBestQuality(
                20, 90, 15_000, q -> new byte[q * 1_000]
        );
        assertFalse(overBudget.reachedTarget);
        assertEquals(20, overBudget.quality);
    }

    @Test
    public void formatSize_andComputeSavingsPercent_formatExpectedStrings() {
        assertEquals("512 B", ImageCompressHelper.formatSize(512));
        assertEquals("94 KB", ImageCompressHelper.formatSize(94 * 1024L));
        assertEquals("3.40 MB", ImageCompressHelper.formatSize(Math.round(3.4 * 1024 * 1024)));

        assertEquals(97, ImageCompressHelper.computeSavingsPercent(3_400_000L, 94_000L));
        assertEquals(0, ImageCompressHelper.computeSavingsPercent(1000L, 1200L));
    }
}
