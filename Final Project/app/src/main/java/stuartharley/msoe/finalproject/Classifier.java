/*
 * Course: SE4910-011
 * Winter 2021
 * Lab: MSOE GPA Calculator
 * Source: https://github.com/oddlyspaced/tensorflowlite-android-sample
 * Edited By: Stuart Harley
 * Edited: 2/14/2022
 */

package stuartharley.msoe.finalproject;

import android.app.Activity;
import android.graphics.Bitmap;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;
import org.tensorflow.lite.support.common.TensorProcessor;
import org.tensorflow.lite.support.common.ops.NormalizeOp;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.image.ops.ResizeOp.ResizeMethod;
import org.tensorflow.lite.support.image.ops.ResizeWithCropOrPadOp;
import org.tensorflow.lite.support.image.ops.Rot90Op;
import org.tensorflow.lite.support.label.TensorLabel;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.util.List;
import java.util.Map;

/**
 * A classifier specialized to label images using TensorFlow Lite.
 */
public class Classifier {
    /**
     * The loaded TensorFlow Lite model.
     */
    private MappedByteBuffer tfliteModel;

    /**
     * Image size along the x axis.
     */
    private final int imageSizeX;

    /**
     * Image size along the y axis.
     */
    private final int imageSizeY;

    /**
     * An instance of the driver class to run model inference with Tensorflow Lite.
     */
    protected Interpreter tflite;

    /**
     * Labels corresponding to the output of the vision model.
     */
    private final List<String> labels;

    /**
     * Input image TensorBuffer.
     */
    private TensorImage inputImageBuffer;

    /**
     * Output probability TensorBuffer.
     */
    private final TensorBuffer outputProbabilityBuffer;

    /**
     * Processor to apply post processing of the output probability.
     */
    private final TensorProcessor probabilityProcessor;

    /**
     * Float MobileNet requires additional normalization of the used input.
     */
    private static final float IMAGE_MEAN = 127.5f;
    private static final float IMAGE_STD = 127.5f;

    /**
     * Float model does not need dequantization in the post-processing. Setting mean and std as 0.0f
     * and 1.0f, repectively, to bypass the normalization.
     */
    private static final float PROBABILITY_MEAN = 0.0f;
    private static final float PROBABILITY_STD = 1.0f;

    /**
     * An immutable result returned by a Classifier describing what was recognized.
     */
    public static class Recognition {
        /**
         * Display name for the recognition.
         */
        private final String title;

        /**
         * A sortable score for how good the recognition is relative to others. Higher should be better.
         */
        private final Float confidence;

        public Recognition(final String title, final Float confidence) {
            this.title = title;
            this.confidence = confidence;
        }

        public String getTitle() { return title; }

        public Float getConfidence() {
            return confidence;
        }
    }

    /**
     * Initializes a Classifier
     */
    public Classifier(Activity activity) throws IOException {
        tfliteModel = FileUtil.loadMappedFile(activity, "model.tflite");
        tflite = new Interpreter(tfliteModel);

        // Loads labels out from the label file.
        labels = FileUtil.loadLabels(activity, "labels.txt");

        // Reads type and shape of input and output tensors, respectively.
        int imageTensorIndex = 0;
        int[] imageShape = tflite.getInputTensor(imageTensorIndex).shape(); // {1, height, width, 3}
        imageSizeY = imageShape[1];
        imageSizeX = imageShape[2];
        DataType imageDataType = tflite.getInputTensor(imageTensorIndex).dataType();
        int probabilityTensorIndex = 0;
        int[] probabilityShape = tflite.getOutputTensor(probabilityTensorIndex).shape(); // {1, NUM_CLASSES}
        DataType probabilityDataType = tflite.getOutputTensor(probabilityTensorIndex).dataType();

        // Creates the input tensor.
        inputImageBuffer = new TensorImage(imageDataType);

        // Creates the output tensor and its processor.
        outputProbabilityBuffer = TensorBuffer.createFixedSize(probabilityShape, probabilityDataType);

        // Creates the post processor for the output probability.
        probabilityProcessor = new TensorProcessor.Builder().add(new NormalizeOp(PROBABILITY_MEAN, PROBABILITY_STD)).build();
    }

    /**
     * Runs inference and returns the classification results.
     */
    public Recognition recognizeImage(final Bitmap bitmap, int sensorOrientation) {
        inputImageBuffer = loadImage(bitmap, sensorOrientation);
        tflite.run(inputImageBuffer.getBuffer(), outputProbabilityBuffer.getBuffer().rewind());

        // Gets the map of label and probability.
        Map<String, Float> labeledProbability = new TensorLabel(
                labels, probabilityProcessor.process(outputProbabilityBuffer)).getMapWithFloatValue();
        // Return the top result
        if(labeledProbability.get("covid") > labeledProbability.get("normal")) {
            return new Recognition("covid", labeledProbability.get("covid"));
        } else {
            return new Recognition("normal", labeledProbability.get("normal"));
        }
    }

    /**
     * Closes the interpreter and model to release resources.
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        tfliteModel = null;
    }

    /**
     * Loads input image, and applies preprocessing.
     */
    private TensorImage loadImage(final Bitmap bitmap, int sensorOrientation) {
        // Loads bitmap into a TensorImage.
        inputImageBuffer.load(bitmap);

        // Creates processor for the TensorImage.
        int cropSize = Math.min(bitmap.getWidth(), bitmap.getHeight());
        int numRotation = sensorOrientation / 90;
        ImageProcessor imageProcessor =
                new ImageProcessor.Builder()
                        .add(new ResizeWithCropOrPadOp(cropSize, cropSize))
                        .add(new ResizeOp(imageSizeX, imageSizeY, ResizeMethod.NEAREST_NEIGHBOR))
                        .add(new Rot90Op(numRotation))
                        .add(new NormalizeOp(IMAGE_MEAN, IMAGE_STD))
                        .build();
        return imageProcessor.process(inputImageBuffer);
    }
}