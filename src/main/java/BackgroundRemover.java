
import java.util.ArrayList;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

// 	astype(np.uint8)
// 	image.shape y, x, color
//	fill() o verschrijft
//	argmax() positie max
// 	getUCharPatch

/**
 * This file contains the reproduction of the Foreground Extraction from Structure Information (FESI) segmentation method from the paper Foreground Extraction for Histopathological Whole-Slide Imaging for comparison with own segmentation method.
 */
public class BackgroundRemover
{
	// /**
	// * """ Create an image writer object.
	// *
	// * Args: file_path (str): File path. dimensions (tuple): Dimensions: (rows, cols). tile_size (int): Size of an I/O tile. pixel_size (tuple, None): Pixel size in micormeters (10^-6 m): width, height. compression (str, int): Compression method
	// * identifier. Values: 'raw' or mrimage.RAW 'jpeg' or mrimage.JPEG 'lzw' or mrimage.LZW 'jpeg2000 lossless' or mrimage.JPEG2000_LOSSLESS 'jpeg2000' or mrimage.JPEG2000_LOSSY data_type (str, int): Data type identifier. Values: 'uint8' or
	// * mrimage.UChar 'uint16' or mrimage.UInt16 'uint32' or mrimage.UInt32 'float' or mrimage.Float color_type (str, int): Color type identifier. Values: 'monochrome' or mrimage.Monochrome 'rgb' or mrimage.RGB 'argb' or mrimage.ARGB 'indexed' or
	// * mrimage.Indexed interpolation (str, int): Interpolation identifier. Values: 'nearest' or mrimage.NearestNeighbor 'linear' or mrimage.Linear image_name (str): Purpose of the image for raising errors.
	// *
	// * Raises: ValueError: String identifier cannot be converted to int representations. IOError: The mr-image file cannot be opened for writing.
	// *
	// * Returns: mrimage.MultiResolutionImageWriter: Image writer object. """
	// *
	// * @return
	// */
	// private void write_mrimage(final String file_path, final int[] dimensions, final double[] pixel_size, final String compression, final String data_type, final String color_type,
	// final String interpolation, final String image_name)
	// {
	// int compression_param;
	// int data_type_param;
	// int color_type_param;
	// int interpolation_param;
	// // Convert string parameters to int.
	// if (compression == "raw")
	// compression_param = mrimage.RAW;
	// else if (compression == "jpeg")
	// compression_param = mrimage.JPEG;
	// else if (compression == "lzw" || compression == null)
	// compression_param = mrimage.LZW;
	// else if (compression == "jpeg2000 lossless")
	// compression_param = mrimage.JPEG2000_LOSSLESS;
	// else if (compression == "jpeg2000")
	// compression_param = mrimage.JPEG2000_LOSSY;
	// else
	// {
	// throw ValueError("Invalid compression", compression);
	// }
	//
	// if (data_type == "uint8" || data_type == null)
	// data_type_param = mrimage.UChar;
	// else if (data_type == "uint16")
	// data_type_param = mrimage.UInt16;
	// else if (data_type == "uint32")
	// data_type_param = mrimage.UInt32;
	// else if (data_type == "float")
	// data_type_param = mrimage.Float;
	// else
	// {
	// throw ValueError("Invalid data type", data_type);
	// }
	//
	// if (color_type == "monochrome")
	// color_type_param = mrimage.Monochrome;
	// else if (color_type == "rgb" || color_type == null)
	// color_type_param = mrimage.RGB;
	// else if (color_type == "argb")
	// color_type_param = mrimage.ARGB;
	// else if (color_type == "indexed")
	// color_type_param = mrimage.Indexed;
	// else
	// {
	// throw ValueError("Invalid color type", color_type);
	// }
	//
	// if (interpolation == "nearest" || interpolation == null)
	// interpolation_param = mrimage.NearestNeighbor;
	// else if (interpolation == "linear")
	// interpolation_param = mrimage.Linear;
	// else
	// {
	// throw ValueError("Invalid interpolation", interpolation);
	// }
	//
	// // Normalize image name: add tailing space if it is not empty.
	// final String norm_image_name = image_name != null && !image_name.isEmpty() ? (image_name + " ") : image_name;
	//
	// // Create image writer object.
	// image_writer = mrimage.MultiResolutionImageWriter();
	//
	// // Open file for writing.
	// final int result_code = image_writer.openFile(file_path);
	// if (result_code != 0)
	// {
	// throw IOError(errno.EACCES, "Cannot write {0}mr-image file".format(norm_image_name), file_path);
	// }
	//
	// // Set parameters.
	// image_writer.setTileSize(512);
	// image_writer.setCompression(compression_param);
	// image_writer.setDataType(data_type_param);
	// image_writer.setColorType(color_type_param);
	// image_writer.setInterpolation(interpolation_param);
	// image_writer.writeImageInformation(dimensions[0], dimensions[1]);
	//
	// if (pixel_size)
	// {
	// pixel_size_vec = mrimage.vector_double();
	// pixel_size_vec.push_back(pixel_size[0]);
	// pixel_size_vec.push_back(pixel_size[1]);
	// image_writer.setSpacing(pixel_size_vec);
	// }
	//
	// // Return the configured object.
	// return image_writer;
	// }
	//

	// ----------------------------------------------------------------------------------------------------

	// /**
	// * """
	// Write an numpy array to mr image file.
	//
	// Args:
	// pixel_array (np.ndarray): Array of pixels to write out.
	// mrimage_path (str): Path of the output file.
	// tile_size (int): Size of an I/O tile.
	// pixel_size (tuple, None): Pixel size in micormeters (10^-6 m): width, height.
	// interpolation (str, int): Interpolation identifier.
	// Values:
	// 'nearest' or mrimage.NearestNeighbor
	// 'linear' or mrimage.Linear
	// image_name (str): Purpose of the image for raising errors.
	//
	// Raises:
	// ValueError: String identifier cannot be converted to int representations.
	// ValueError: Invalid 3rd image dimension.
	// IOError: The mr-image file cannot be opened for writing.
	// """
	//
	// */
	// private void write_array_to_mrimage(final pixel_array, mrimage_path, int tile_size=512, int pixel_size=None, String interpolation='nearest', String image_name='')
	// {
	//
	// // Get the shape of the image and infer the color depth.
	// //
	// int[] array_shape = pixel_array.shape;
	// array_dtype = pixel_array.dtype;
	//
	// // Check shape. The third dimension have to be either 1 or 3, or there should only be 2 dimensions.
	// //
	// if (!(array_shape.length == 2 || (array_shape.length == 3 && (array_shape[2] == 1 || array_shape[2] == 3))))
	// {
	// throw new ValueError("Invalid 3rd dimension", array_shape);
	// }
	//
	// int[] image_dimensions={array_shape[1],array_shape[0]};
	// String image_color_type=array_shape.length == 3 && array_shape[2] == 3 ? "rgb" : "monochrome";
	// String image_compression = image_color_type == "monochrome" ? "lzw" : "jpeg";
	//
	// // Find data type to use.
	// String image_dtype;
	// if(array_dtype == np.uint8)
	// {
	// image_dtype = "uint8";
	// }
	// else if(array_dtype == np.uint16)
	// {
	// image_dtype = "uint16";
	// }
	// else if(array_dtype == np.uint32)
	// {
	// image_dtype = "uint32";
	// }
	// else
	// {
	// image_dtype = "float";
	// }
	//
	// // Initialize image writer.
	// image_writer = write_mrimage(mrimage_path,
	// image_dimensions,
	// tile_size,
	// pixel_size,
	// compression=image_compression,
	// image_dtype,
	// image_color_type,
	// interpolation,
	// image_name);
	//
	// // Write pixel data to the mrimage patch by patch.
	// int[] tile_shape = {tile_size, tile_size};
	// for(int y = 0; y < image_dimensions[1]; y+= tile_size)
	// {
	// for(int x = 0; x < image_dimensions[0]; x += tile_size)
	// {
	// image_writer.writeBaseImagePart(patchop.left_bottom_pad_patch(pixel_array[y: y+tile_size, x: x+tile_size], tile_shape).flatten());
	// }
	// }
	//
	// // Finalize the output image.
	// //
	// image_writer.finishImage();
	// }

	// ----------------------------------------------------------------------------------------------------

	/**
	 * """ Calculate the tissue mask using the FESI algorithm with default parameters.
	 *
	 * Args: image_path (str): Input image path. result_path (str): Result mask image path. image_level (int): Mask processing level. verbose (bool): Flag controlling printout. If false no print is made to the stdout. """
	 *
	 * @return
	 */
	private static Mat calculate_fesi_mask(final Mat aImage)
	{

		// Open the image and calculate the mask.
		return fesi_select_foreground(aImage);

		// Calculate pixel size.
		// final double[] image_spacing = mr_image.getSpacing();
		// final double downsampling = mr_image.getLevelDownsample(image_level);
		// final double[] image_pixel_size = { image_spacing[0] * downsampling, image_spacing[1] * downsampling };

		// Write out the result to multiresolution TIF file.
		// write_array_to_mrimage(mask_array, result_path, 512, image_pixel_size, "nearest");
	}


	// ----------------------------------------------------------------------------------------------------

	/**
	 * """ Select foreground with the described FESI method from the image and returns the ndarray representing the result.
	 *
	 * The steps are: 1. Load the image and apply a bit of preprocessing to remove the completely back areas that are background and should be white. 2. Apply Laplacian filter on the preprocessed image. 3. Apply Gaussian blur to the absolute value of
	 * the Laplacian image. 4. Apply median blur and then morphological opening on the Gaussian blurred image. 5. Apply distance transformation on the inverse mask and select maximal value as seed point. 6. Flood fill the mask of step 4. from the
	 * previously identified seed point with 0 fillinig value to select the initial mask. 7. Apply distance transformation on the initial mask. 8. Loop over the maximal distance values in the distance image of step 7. 8.1 Select the maximal distance
	 * point as seed point. 8.2 Clear the region marked by the seed point from the mask and the distance values. 8.3 If current maximal distance is large enough (=the region is large enough) or it is close enough to any previous seed point mark the
	 * region in the final mask as foreground. 9. Finalize the maks by removing all non-marked regions.
	 *
	 * Args: mr_image (mrimage.MultiResolutionImage): Input image to process. level (int): Processing level. gaussian_kernel_size (int): Kerenel size for Gaussian blurring. gaussian_sigma (float): Sigma for Gaussian blurring. median_kernel_size
	 * (int): Median filter kernel size. opening_kernel_size (int): Morphological opening kernel size. opening_iterations (int): Morphological opening iteration count. seed_distance_map_mask_size (int): Mask size for distance map calculation in seed
	 * point search. distance_map_mask_size (int): Mask size for distance map calculation. distance_threshold (float): Distance threshold to select the large enough regions.
	 *
	 * Returns: np.ndarray: Result mask where the foreground pixels are marked as 255 and the background pixels are 0. """
	 *
	 * @return
	 *
	 */
	public static Mat fesi_select_foreground(final Mat mr_image)
	{
		final int gaussian_kernel_size = 15;
		final double gaussian_sigma = 4.0;
		final int median_kernel_size = 45;
		final int opening_kernel_size = 7;
		final int opening_iterations = 5;
		final int seed_distance_map_mask_size = 3;
		final int distance_map_mask_size = 5;
		final double distance_threshold = 10.0; // The minimum width a section must have to be considered a part of the foreground

		// Build tuples from sizes that is required by OpenCV.
		//
		final Size gaussian_kernel_shape = new Size(gaussian_kernel_size, gaussian_kernel_size);
		final Size opening_kernel_shape = new Size(opening_kernel_size, opening_kernel_size);

		// Load RGB image content to array and convert it to grayscale.
		final Mat image_array = new Mat();
		Imgproc.cvtColor(mr_image, image_array, Imgproc.COLOR_RGB2GRAY);

		// Get the initial foreground mask.
		final Mat laplace = new Mat();
		Imgproc.Laplacian(image_array, laplace, CvType.CV_32F);
		for (int y = 0; y < laplace.size().height; y++)
		{
			for (int x = 0; x < laplace.size().width; x++)
			{
				final double val = laplace.get(y, x)[0];
				laplace.put(y, x, Math.abs(val));
			}
		}

		final Mat blurred = new Mat();
		Imgproc.GaussianBlur(laplace, blurred, gaussian_kernel_shape, gaussian_sigma);
		final double blurred_mean = Core.mean(blurred).val[0];
		Mat mask = new Mat();
		Mat temp_mask = new Mat();
		Imgproc.threshold(blurred, temp_mask, blurred_mean, 150.0, Imgproc.THRESH_BINARY);
		temp_mask.convertTo(mask, CvType.CV_8U);

		// Refine foreground mask with with morphological opening.
		temp_mask = new Mat();
		Imgproc.medianBlur(mask, temp_mask, median_kernel_size);
		for (int y = 0; y < temp_mask.size().height; y++)
		{
			for (int x = 0; x < temp_mask.size().width; x++)
			{
				final double val = temp_mask.get(y, x)[0];
				temp_mask.put(y, x, val + 100);
			}
		}
		mask = new Mat();
		Imgproc.morphologyEx(temp_mask, mask, Imgproc.MORPH_OPEN, Imgproc.getStructuringElement(Imgproc.MORPH_ELLIPSE, opening_kernel_shape), new Point(-1, -1), opening_iterations);

		// Select a point from background with distance transformation for seed point for flood fill.
		//
		final Mat inverse_mask = new Mat();
		temp_mask = new Mat();
		Imgproc.threshold(blurred, temp_mask, blurred_mean, 255.0, Imgproc.THRESH_BINARY_INV);
		temp_mask.convertTo(inverse_mask, CvType.CV_8UC1);
		final Mat distance_map = new Mat();
		Imgproc.distanceTransform(inverse_mask, distance_map, Imgproc.CV_DIST_C, seed_distance_map_mask_size);

		final Point flood_seed = new Point();
		getMatMax(distance_map, flood_seed);

		// Apply flood fill on the mask from the selected background point for refined foreground mask.
		//
		Mat floodfill_mask = Mat.zeros(((int) mask.size().height) + 2, ((int) mask.size().width) + 2, CvType.CV_8UC1);
		Imgproc.floodFill(mask, floodfill_mask, flood_seed, new Scalar(0));
		for (int y = 0; y < mask.size().height; y++)
		{
			for (int x = 0; x < mask.size().width; x++)
			{
				final double val = mask.get(y, x)[0];
				if (val > 1)
				{
					mask.put(y, x, 255);
				}
			}
		}

		// Go through the distinctive regions and select the large enough or close enough ones.
		//
		final Mat final_mask = mask.clone();

		final Mat distance = new Mat();
		Imgproc.distanceTransform(mask, distance, Imgproc.CV_DIST_C, distance_map_mask_size);

		final Point start = new Point();
		double dist_max = getMatMax(distance, start);

		final List<double[]> seeds = new ArrayList<>();
		while (0 < dist_max)
		{
			getMatMax(distance, start);
			final double[] nextElem = { start.x, start.y, dist_max };
			seeds.add(nextElem);

			floodfill_mask = Mat.zeros(floodfill_mask.size(), floodfill_mask.type());
			Imgproc.floodFill(mask, floodfill_mask, start, new Scalar(0));
			for (int y = 0; y < distance.size().height; y++)
			{
				for (int x = 0; x < distance.size().width; x++)
				{
					final double val = mask.get(y, x)[0]; // Yes, mask, not distance
					if (val == 0)
					{
						distance.put(y, x, 0);
					}
				}
			}
			dist_max = getMatMax(distance, new Point());
			if ((dist_max > distance_threshold) || is_close(seeds, start, distance_threshold))
			{
				floodfill_mask = Mat.zeros(floodfill_mask.size(), floodfill_mask.type());
				Imgproc.floodFill(final_mask, floodfill_mask, start, new Scalar(200));
			}
		}

		// Finalize the mask: clear everything that is not marked as foreground so far.
		for (int y = 0; y < final_mask.size().height; y++)
		{
			for (int x = 0; x < final_mask.size().width; x++)
			{
				final double val = final_mask.get(y, x)[0];
				final_mask.put(y, x, val == 200 ? 255 : 0);
			}
		}

		return final_mask;
	}


	// ----------------------------------------------------------------------------------------------------

	private static double getMatMax(final Mat aMat, final Point aPoint)
	{
		double max = -1;
		for (int y = 0; y < aMat.size().height; y++)
		{
			for (int x = 0; x < aMat.size().width; x++)
			{
				final double val = aMat.get(y, x)[0];
				if (val > max)
				{
					max = val;
					aPoint.x = x;
					aPoint.y = y;
				}
			}
		}

		return max;
	}


	// ----------------------------------------------------------------------------------------------------

	private static Mat imageToMat(final ImagePlus aImage)
	{
		final int width = aImage.getWidth();
		final int height = aImage.getHeight();
		final Mat result = new Mat(height, width, CvType.CV_8UC3);
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				final int[] pixel = aImage.getPixel(x, y);
				result.put(y, x, pixel[0], pixel[1], pixel[2]);
			}
		}

		return result;
	}


	// ----------------------------------------------------------------------------------------------------

	/**
	 * """ Check if the seed points are close enough to the start.
	 *
	 * Args: seeds (list): Seed points. start (tuple): Start point. distance_threshold (float): Distance threshold.
	 *
	 * Returns: bool: True if the seed points close enough, false otherwise. """
	 *
	 * @return
	 */
	private static boolean is_close(final List<double[]> seeds, final Point start, final double distance_threshold)
	{
		for (final double[] seed : seeds)
		{
			if ((Math.pow(seed[0] - start.x, 2) + Math.pow(seed[1] - start.y, 2) < Math.pow(3 * seed[2], 2)) && (seed[2] > distance_threshold))
			{
				return true;
			}
		}
		return false;
	}


	private static ImagePlus matToImagePlus(final Mat aImage)
	{
		final int width = (int) aImage.size().width;
		final int height = (int) aImage.size().height;
		final ImagePlus result = IJ.createImage("Background mask", "8-bit black", width, height, 1);
		final ImageProcessor proc = result.getProcessor();
		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				final int pixel = (int) aImage.get(y, x)[0];
				proc.set(x, y, pixel);
			}
		}

		return result;
	}


	public static ImagePlus removeBackground(final ImagePlus aImage)
	{
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		// Process the image.
		final Mat resultMat = calculate_fesi_mask(imageToMat(aImage));

		return matToImagePlus(resultMat);
	}

}
